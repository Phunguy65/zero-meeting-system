package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestDeniedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingEndedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.RedisSseEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles meeting end events by auto-denying all pending join requests.
 *
 * <p>Listens to {@link MeetingEndedEvent} and cleans up the join request queue
 * for the ended meeting.
 */
@Component
public class MeetingEndedJoinRequestHandler {

    private static final Logger log = LoggerFactory.getLogger(MeetingEndedJoinRequestHandler.class);

    private final JoinRequestRepository joinRequestRepository;
    private final RedisSseEventPublisher sseEventPublisher;

    public MeetingEndedJoinRequestHandler(
            JoinRequestRepository joinRequestRepository,
            RedisSseEventPublisher sseEventPublisher) {
        this.joinRequestRepository = joinRequestRepository;
        this.sseEventPublisher = sseEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MeetingEndedEvent event) {
        UUID meetingId = event.aggregateId();

        // Get all pending requests
        List<JoinRequest> pendingRequests = joinRequestRepository.findPendingByMeetingId(meetingId);

        if (pendingRequests.isEmpty()) {
            return;
        }

        log.info("Auto-denying {} pending join requests for ended meeting {}", 
                pendingRequests.size(), meetingId);

        // Deny each request and publish SSE event
        for (JoinRequest joinRequest : pendingRequests) {
            // Publish denied event to SSE (no domain event needed, meeting already ended)
            Map<String, Object> sseData = new HashMap<>();
            sseData.put("requestId", joinRequest.getId().value().toString());
            sseData.put("status", "DENIED");
            sseData.put("reason", "meeting_ended");
            sseEventPublisher.publish(meetingId, "join_request_denied", sseData);
        }

        // Delete all join requests for this meeting
        joinRequestRepository.deleteAllByMeetingId(meetingId);
    }
}
