package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestApprovedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
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
public class ApproveJoinRequestUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final LiveKitPort liveKitPort;
    private final RedisSseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApproveJoinRequestUseCase(
            MeetingRepository meetingRepository,
            JoinRequestRepository joinRequestRepository,
            LiveKitPort liveKitPort,
            RedisSseEventPublisher sseEventPublisher,
            ApplicationEventPublisher applicationEventPublisher) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.liveKitPort = liveKitPort;
        this.sseEventPublisher = sseEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Result<String, MeetingError> execute(ApproveJoinRequestCommand command) {
        var meetingOpt = meetingRepository.findById(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        // Validate host
        if (!meeting.getHostId().equals(UserId.of(command.approvedBy()))) {
            return Result.failure(
                    new MeetingError.NotAuthorized(command.approvedBy(), meeting.getHostId().value()));
        }

        // Load join request
        var requestOpt = joinRequestRepository.findById(command.requestId());
        if (requestOpt.isEmpty()) {
            return Result.failure(
                    new MeetingError.JoinRequestNotFound(command.meetingId(), command.requestId()));
        }
        var joinRequest = requestOpt.get();

        // Approve (handles idempotency and invalid transitions)
        var approveResult = joinRequest.approve();
        if (approveResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        // Generate LiveKit token
        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());
        ParticipantRole role = joinRequest.getUserId().isPresent()
                ? ParticipantRole.PARTICIPANT
                : ParticipantRole.GUEST;

        LiveKitIdentity identity = joinRequest.getUserId().isPresent()
                ? LiveKitIdentity.fromUser(joinRequest.getUserId().get(), joinRequest.getDeviceId())
                : LiveKitIdentity.forGuest(joinRequest.getDeviceId());

        var tokenResult = liveKitPort.generateToken(
                roomName, identity, joinRequest.getDisplayName(), role);
        if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }
        String token = ((Result.Success<String, MeetingError>) tokenResult).value();

        // Update status in Redis
        joinRequestRepository.updateStatus(command.requestId(), JoinRequestStatus.APPROVED);

        // Publish domain event (Kafka outbox)
        var approvedEvent = new JoinRequestApprovedEvent(
                UUID.randomUUID(),
                command.meetingId(),
                command.requestId(),
                command.approvedBy(),
                Instant.now());
        applicationEventPublisher.publishEvent(approvedEvent);

        // Publish SSE event (Redis Pub/Sub)
        Map<String, Object> sseData = new HashMap<>();
        sseData.put("requestId", command.requestId().toString());
        sseData.put("status", "APPROVED");
        sseData.put("approvedBy", command.approvedBy().toString());
        sseEventPublisher.publish(command.meetingId(), "join_request_approved", sseData);

        // Remove from queue (approved requests don't need to stay in queue)
        joinRequestRepository.removeFromQueue(command.meetingId(), command.requestId());

        return Result.success(token);
    }
}
