package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.DenyJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestDeniedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.RedisSseEventPublisher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class DenyJoinRequestUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final RedisSseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public DenyJoinRequestUseCase(
            MeetingRepository meetingRepository,
            JoinRequestRepository joinRequestRepository,
            RedisSseEventPublisher sseEventPublisher,
            ApplicationEventPublisher applicationEventPublisher) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.sseEventPublisher = sseEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Result<Void, MeetingError> execute(DenyJoinRequestCommand command) {
        var meetingOpt = meetingRepository.findById(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        // Validate host
        if (!meeting.getHostId().equals(UserId.of(command.deniedBy()))) {
            return Result.failure(
                    new MeetingError.NotAuthorized(command.deniedBy(), meeting.getHostId().value()));
        }

        // Load join request
        var requestOpt = joinRequestRepository.findById(command.requestId());
        if (requestOpt.isEmpty()) {
            return Result.failure(
                    new MeetingError.JoinRequestNotFound(command.meetingId(), command.requestId()));
        }
        var joinRequest = requestOpt.get();

        // Deny (handles idempotency and invalid transitions)
        var denyResult = joinRequest.deny();
        if (denyResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        // Update status in Redis
        joinRequestRepository.updateStatus(command.requestId(), JoinRequestStatus.DENIED);

        // Publish domain event
        var deniedEvent = new JoinRequestDeniedEvent(
                UUID.randomUUID(),
                command.meetingId(),
                command.requestId(),
                command.deniedBy(),
                Instant.now());
        applicationEventPublisher.publishEvent(deniedEvent);

        // Publish SSE event
        Map<String, Object> sseData = new HashMap<>();
        sseData.put("requestId", command.requestId().toString());
        sseData.put("status", "DENIED");
        sseEventPublisher.publish(command.meetingId(), "join_request_denied", sseData);

        // Remove from queue
        joinRequestRepository.removeFromQueue(command.meetingId(), command.requestId());

        return Result.success();
    }
}
