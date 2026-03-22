package io.github.phunguy65.zms.meetingmanagement.infrastructure.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE connections for meeting hosts and broadcasts join request events.
 *
 * <p>Holds SSE emitters in-memory per meeting and subscribes to Redis Pub/Sub
 * channel {@code meeting:{meetingId}:events} to receive events from any backend instance.
 *
 * <p>Thread-safe: uses {@link ConcurrentHashMap} for meeting-level storage and
 * {@link CopyOnWriteArrayList} for per-meeting emitter lists.
 */
@Component
public class MeetingSseManager {

    private static final Logger log = LoggerFactory.getLogger(MeetingSseManager.class);
    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<SseEmitter>> emittersByMeeting =
            new ConcurrentHashMap<>();
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    public MeetingSseManager(
            RedisMessageListenerContainer listenerContainer, ObjectMapper objectMapper) {
        this.listenerContainer = listenerContainer;
        this.objectMapper = objectMapper;

        listenerContainer.addMessageListener(
                new RedisEventListener(), new ChannelTopic("meeting:*:events"));
    }

    /**
     * Subscribes a host to SSE events for a meeting.
     *
     * @param meetingId the meeting ID
     * @param userId the host user ID (for logging/debugging)
     * @return an {@link SseEmitter} that will receive events
     */
    public SseEmitter subscribe(UUID meetingId, UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emittersByMeeting.computeIfAbsent(meetingId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(meetingId, emitter));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout for meeting {} user {}", meetingId, userId);
            removeEmitter(meetingId, emitter);
        });
        emitter.onError(e -> {
            log.warn("SSE error for meeting {} user {}: {}", meetingId, userId, e.getMessage());
            removeEmitter(meetingId, emitter);
        });

        log.debug("Host {} subscribed to SSE for meeting {}", userId, meetingId);
        return emitter;
    }

    private void removeEmitter(UUID meetingId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByMeeting.get(meetingId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByMeeting.remove(meetingId);
            }
        }
    }

    /**
     * Redis Pub/Sub listener that broadcasts messages to matching SSE emitters.
     */
    private class RedisEventListener implements MessageListener {
        @Override
        public void onMessage(Message message, byte[] pattern) {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            String[] parts = channel.split(":");
            if (parts.length != 3) {
                log.warn("Invalid channel format: {}", channel);
                return;
            }

            UUID meetingId;
            try {
                meetingId = UUID.fromString(parts[1]);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid meetingId in channel {}: {}", channel, parts[1]);
                return;
            }

            SseEvent event;
            try {
                event = objectMapper.readValue(body, SseEvent.class);
            } catch (IOException e) {
                log.error("Failed to parse SSE event from Redis: {}", body, e);
                return;
            }

            CopyOnWriteArrayList<SseEmitter> emitters = emittersByMeeting.get(meetingId);
            if (emitters == null || emitters.isEmpty()) {
                return;
            }

            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(
                            SseEmitter.event()
                                    .name(event.type())
                                    .data(event.data()));
                } catch (IOException e) {
                    log.debug("Failed to send SSE event to emitter: {}", e.getMessage());
                    deadEmitters.add(emitter);
                }
            }

            deadEmitters.forEach(emitter -> removeEmitter(meetingId, emitter));
        }
    }
}
