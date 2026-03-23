package io.github.phunguy65.zms.meetingmanagement.presentation;

import io.github.phunguy65.zms.meetingmanagement.application.command.AssignSidCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.CloseStaleMeetingLogsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.command.LeaveMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.AssignSidUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.CloseStaleMeetingLogsUseCase;
import io.github.phunguy65.zms.meetingmanagement.application.usecase.LeaveMeetingUseCase;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.config.LiveKitProperties;
import io.livekit.server.WebhookReceiver;
import java.util.UUID;
import livekit.LivekitWebhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives LiveKit server-side webhook events and delegates to the appropriate use cases.
 *
 * <p>LiveKit POSTs events to this endpoint with {@code Content-Type: application/webhook+json}.
 * The request body is the raw JSON payload; the {@code Authorization} header contains a signed
 * JWT whose {@code sha256} claim is a hash of the raw body. The {@link WebhookReceiver} from the
 * LiveKit Java SDK verifies both the JWT signature and the body hash before processing.
 *
 * <p>This endpoint is {@code permitAll} in {@link
 * io.github.phunguy65.zms.meetingmanagement.infrastructure.security.SecurityConfig} because
 * LiveKit requests do not carry an {@code X-User-ID} header. Security is provided entirely by
 * the webhook signature verification.
 *
 * <p>The endpoint <b>always returns HTTP 200</b> after verification, even if a use case reports
 * a non-fatal condition (e.g. log not found). Any non-2xx response would cause LiveKit to retry
 * the event, which is undesirable for idempotent warnings.
 *
 * <p>Handled events:
 * <ul>
 *   <li>{@code participant_joined} — assigns the LiveKit participant SID to the pending
 *       participation log created at token-issuance time.</li>
 *   <li>{@code participant_left} — records the participant's departure.</li>
 *   <li>{@code room_finished} — bulk-closes any remaining active participation logs for the
 *       meeting (safety net for missed {@code participant_left} events).</li>
 * </ul>
 */
@RestController
public class LiveKitWebhookController {

    private static final Logger log = LoggerFactory.getLogger(LiveKitWebhookController.class);

    private static final String ROOM_NAME_PREFIX = "meeting-";

    private final WebhookReceiver webhookReceiver;
    private final AssignSidUseCase assignSidUseCase;
    private final LeaveMeetingUseCase leaveMeetingUseCase;
    private final CloseStaleMeetingLogsUseCase closeStaleMeetingLogsUseCase;

    public LiveKitWebhookController(
            LiveKitProperties liveKitProperties,
            AssignSidUseCase assignSidUseCase,
            LeaveMeetingUseCase leaveMeetingUseCase,
            CloseStaleMeetingLogsUseCase closeStaleMeetingLogsUseCase) {
        this.webhookReceiver = new WebhookReceiver(
                liveKitProperties.getApiKey(), liveKitProperties.getApiSecret());
        this.assignSidUseCase = assignSidUseCase;
        this.leaveMeetingUseCase = leaveMeetingUseCase;
        this.closeStaleMeetingLogsUseCase = closeStaleMeetingLogsUseCase;
    }

    /**
     * Receives a LiveKit webhook event.
     *
     * <p>The {@code @RequestBody String rawBody} binding is intentional — Spring must NOT parse
     * the JSON before this method receives it. The LiveKit SDK verifies the SHA-256 hash of the
     * exact raw bytes; any framework-level JSON parsing (whitespace normalization, field reordering)
     * would break signature verification.
     */
    @PostMapping(
            value = "/webhook/livekit",
            consumes = {"application/webhook+json", "application/json"})
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        LivekitWebhook.WebhookEvent event;
        try {
            event = webhookReceiver.receive(rawBody, authHeader);
        } catch (Exception e) {
            log.warn("LiveKit webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }

        String eventType = event.getEvent();
        log.debug("Received LiveKit webhook event: {}", eventType);

        try {
            switch (eventType) {
                case "participant_joined" -> handleParticipantJoined(event);
                case "participant_left" -> handleParticipantLeft(event);
                case "room_finished" -> handleRoomFinished(event);
                default -> log.trace("Ignoring LiveKit webhook event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error(
                    "Unexpected error processing LiveKit webhook event '{}': {}",
                    eventType,
                    e.getMessage(),
                    e);
        }

        return ResponseEntity.ok().build();
    }

    private void handleParticipantJoined(LivekitWebhook.WebhookEvent event) {
        var meetingId = extractMeetingId(event.getRoom().getName());
        if (meetingId == null) return;

        var participant = event.getParticipant();
        assignSidUseCase.execute(
                new AssignSidCommand(meetingId, participant.getIdentity(), participant.getSid()));
    }

    private void handleParticipantLeft(LivekitWebhook.WebhookEvent event) {
        var meetingId = extractMeetingId(event.getRoom().getName());
        if (meetingId == null) return;

        var sid = event.getParticipant().getSid();
        leaveMeetingUseCase.execute(new LeaveMeetingCommand(meetingId, sid));
    }

    private void handleRoomFinished(LivekitWebhook.WebhookEvent event) {
        var meetingId = extractMeetingId(event.getRoom().getName());
        if (meetingId == null) return;

        closeStaleMeetingLogsUseCase.execute(new CloseStaleMeetingLogsCommand(meetingId));
    }

    /**
     * Extracts the meeting UUID from a LiveKit room name.
     *
     * <p>Room names are formatted as {@code "meeting-{uuid}"} by
     * {@link io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName#fromMeetingId}.
     *
     * @return the parsed UUID, or {@code null} if the room name is not a managed meeting room
     */
    private UUID extractMeetingId(String roomName) {
        if (roomName == null || !roomName.startsWith(ROOM_NAME_PREFIX)) {
            log.debug("Ignoring webhook for non-meeting room: {}", roomName);
            return null;
        }
        String uuidPart = roomName.substring(ROOM_NAME_PREFIX.length());
        try {
            return UUID.fromString(uuidPart);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Could not parse meeting UUID from room name '{}': {}",
                    roomName,
                    e.getMessage());
            return null;
        }
    }
}
