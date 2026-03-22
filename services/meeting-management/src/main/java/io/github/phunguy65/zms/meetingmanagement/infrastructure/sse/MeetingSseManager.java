package io.github.phunguy65.zms.meetingmanagement.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestApprovedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestCreatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestDeniedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestExpiredEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.config.SseProperties;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.model.*;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages SSE connections for meeting hosts and guests, broadcasting join request lifecycle events.
 *
 * <p>Maintains two emitter registries:
 *
 * <ul>
 *   <li>{@code hostEmittersByMeeting} — meeting-scoped emitters for hosts watching new join
 *       requests
 *   <li>{@code guestEmittersByRequest} — request-scoped emitters for guests waiting on approval
 * </ul>
 *
 * <p>Consumes CloudEvents from four Kafka topics using a unique consumer group per instance
 * (configured in {@link io.github.phunguy65.zms.meetingmanagement.infrastructure.config.KafkaConfig})
 * to ensure all instances receive all events for SSE fan-out.
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} + {@link CopyOnWriteArrayList} for emitter
 * storage.
 */
@Component
public class MeetingSseManager {

    private static final Logger log = LoggerFactory.getLogger(MeetingSseManager.class);

    /** HOST emitters keyed by meetingId. Each meeting may have many connected host clients. */
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> hostEmittersByMeeting =
            new ConcurrentHashMap<>();

    /** GUEST emitters keyed by requestId. Each join request has at most one guest SSE stream. */
    private final ConcurrentHashMap<UUID, SseEmitter> guestEmittersByRequest =
            new ConcurrentHashMap<>();

    private final SseProperties sseProperties;
    private final ObjectMapper objectMapper;

    public MeetingSseManager(SseProperties sseProperties, ObjectMapper objectMapper) {
        this.sseProperties = sseProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribes a host to SSE events for a meeting.
     *
     * <p>Timeout is read from {@link SseProperties#getTimeoutMs()} at connection time. Changes to
     * Consul KV apply to new connections only; existing connections retain their original timeout.
     *
     * @param meetingId the meeting ID
     * @param userId the host user ID (for logging/debugging)
     * @return an {@link SseEmitter} that will receive join-request lifecycle events
     */
    public SseEmitter subscribeHost(UUID meetingId, UUID userId) {
        SseEmitter emitter = new SseEmitter(sseProperties.getTimeoutMs());

        hostEmittersByMeeting
                .computeIfAbsent(meetingId, k -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeHostEmitter(meetingId, emitter));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for host meeting={} user={}", meetingId, userId);
            removeHostEmitter(meetingId, emitter);
        });
        emitter.onError(e -> {
            log.warn(
                    "SSE error for host meeting={} user={}: {}", meetingId, userId, e.getMessage());
            removeHostEmitter(meetingId, emitter);
        });

        log.debug("Host {} subscribed to SSE for meeting {}", userId, meetingId);
        return emitter;
    }

    /**
     * Subscribes a guest to SSE events for a specific join request.
     *
     * <p>Timeout is read from {@link SseProperties#getJoinRequestTimeoutMs()} at connection time.
     * The stream is closed automatically once the request is resolved (approved/denied/expired).
     *
     * @param requestId the join request ID
     * @return an {@link SseEmitter} that will receive the resolution event and then close
     */
    public SseEmitter subscribeGuest(UUID requestId) {
        SseEmitter emitter = new SseEmitter(sseProperties.getJoinRequestTimeoutMs());

        guestEmittersByRequest.put(requestId, emitter);

        emitter.onCompletion(() -> guestEmittersByRequest.remove(requestId));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for guest request={}", requestId);
            guestEmittersByRequest.remove(requestId);
        });
        emitter.onError(e -> {
            log.warn("SSE error for guest request={}: {}", requestId, e.getMessage());
            guestEmittersByRequest.remove(requestId);
        });

        log.debug("Guest subscribed to SSE for join request {}", requestId);
        return emitter;
    }

    /**
     * Handles {@code join_request.created} events: notifies all connected hosts for the meeting.
     */
    @KafkaListener(
            topics = "meeting-management.join_request.created",
            containerFactory = "cloudEventKafkaListenerContainerFactory")
    public void onJoinRequestCreated(CloudEvent cloudEvent) {
        JoinRequestCreatedEvent event = deserialize(cloudEvent, JoinRequestCreatedEvent.class);
        if (event == null) return;

        UUID meetingId = event.meetingId();
        SseEventData sseData = new JoinRequestCreatedData(
                event.joinRequestId().toString(), meetingId.toString(), event.displayName());

        pushToHostEmitters(meetingId, "join_request_created", sseData);
    }

    /**
     * Handles {@code join_request.approved} events: delivers the LiveKit token to the guest SSE
     * stream and closes the stream.
     */
    @KafkaListener(
            topics = "meeting-management.join_request.approved",
            containerFactory = "cloudEventKafkaListenerContainerFactory")
    public void onJoinRequestApproved(CloudEvent cloudEvent) {

        final String eventName = "join_request_approved";

        JoinRequestApprovedEvent event = deserialize(cloudEvent, JoinRequestApprovedEvent.class);
        if (event == null) return;

        UUID requestId = event.joinRequestId();
        SseEventData sseData = new JoinRequestApprovedData(
                requestId.toString(), JoinRequestStatus.APPROVED.name(), event.liveKitToken());

        sendToGuestAndComplete(requestId, eventName, sseData);
    }

    /**
     * Handles {@code join_request.denied} events: notifies the guest and closes the stream.
     */
    @KafkaListener(
            topics = "meeting-management.join_request.denied",
            containerFactory = "cloudEventKafkaListenerContainerFactory")
    public void onJoinRequestDenied(CloudEvent cloudEvent) {
        final String eventName = "join_request_denied";

        JoinRequestDeniedEvent event = deserialize(cloudEvent, JoinRequestDeniedEvent.class);
        if (event == null) return;

        UUID requestId = event.joinRequestId();
        SseEventData sseData =
                new JoinRequestDeniedData(requestId.toString(), JoinRequestStatus.DENIED.name());

        sendToGuestAndComplete(requestId, eventName, sseData);
    }

    /**
     * Handles {@code join_request.expired} events: notifies the host (meeting-level) AND the guest
     * (request-level), then closes the guest stream.
     */
    @KafkaListener(
            topics = "meeting-management.join_request.expired",
            containerFactory = "cloudEventKafkaListenerContainerFactory")
    public void onJoinRequestExpired(CloudEvent cloudEvent) {
        final String eventName = "join_request_expired";
        JoinRequestExpiredEvent event = deserialize(cloudEvent, JoinRequestExpiredEvent.class);
        if (event == null) return;

        UUID meetingId = event.meetingId();
        UUID requestId = event.joinRequestId();
        SseEventData sseData =
                new JoinRequestExpiredData(requestId.toString(), JoinRequestStatus.EXPIRED.name());

        pushToHostEmitters(meetingId, eventName, sseData);

        sendToGuestAndComplete(requestId, eventName, sseData);
    }

    /** Push an event to all HOST emitters for a meeting; remove dead emitters on IOException. */
    private void pushToHostEmitters(UUID meetingId, String eventName, SseEventData data) {
        CopyOnWriteArrayList<SseEmitter> emitters = hostEmittersByMeeting.get(meetingId);
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("Dead host emitter for meeting={}: {}", meetingId, e.getMessage());
                dead.add(emitter);
            }
        }
        dead.forEach(emitter -> removeHostEmitter(meetingId, emitter));
    }

    /** Send an event to a GUEST emitter, complete it, and remove from registry. */
    private void sendToGuestAndComplete(UUID requestId, String eventName, SseEventData data) {
        SseEmitter emitter = guestEmittersByRequest.get(requestId);
        if (emitter == null) return;

        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
            emitter.complete();
        } catch (IOException e) {
            log.debug("Dead guest emitter for request={}: {}", requestId, e.getMessage());
        } finally {
            guestEmittersByRequest.remove(requestId);
        }
    }

    private void removeHostEmitter(UUID meetingId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = hostEmittersByMeeting.get(meetingId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                hostEmittersByMeeting.remove(meetingId);
            }
        }
    }

    /** Deserialize CloudEvent data payload to the given type; returns null on failure. */
    private <T> T deserialize(CloudEvent cloudEvent, Class<T> type) {
        if (cloudEvent.getData() == null) {
            log.warn("Received CloudEvent with no data payload: {}", cloudEvent.getId());
            return null;
        }
        try {
            byte[] data = cloudEvent.getData().toBytes();
            return objectMapper.readValue(data, type);
        } catch (IOException e) {
            log.error(
                    "Failed to deserialize CloudEvent {} to {}: {}",
                    cloudEvent.getId(),
                    type.getSimpleName(),
                    e.getMessage());
            return null;
        }
    }
}
