package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveAllJoinRequestsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.ApproveAllResponse;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApproveAllJoinRequestsUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final LiveKitPort liveKitPort;
    private final RedisSseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApproveAllJoinRequestsUseCase(
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
    public Result<ApproveAllResponse, MeetingError> execute(ApproveAllJoinRequestsCommand command) {
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

        // Load all pending requests
        List<JoinRequest> pendingRequests =
                joinRequestRepository.findPendingByMeetingId(command.meetingId());

        if (pendingRequests.isEmpty()) {
            return Result.success(new ApproveAllResponse(0));
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());
        int approvedCount = 0;

        for (JoinRequest joinRequest : pendingRequests) {
            // Approve
            var approveResult = joinRequest.approve();
            if (approveResult instanceof Result.Failure<?, MeetingError>) {
                continue; // Skip invalid transitions
            }

            // Generate token (not stored, participant will poll to get it)
            ParticipantRole role = joinRequest.getUserId().isPresent()
                    ? ParticipantRole.PARTICIPANT
                    : ParticipantRole.GUEST;

            LiveKitIdentity identity = joinRequest.getUserId().isPresent()
                    ? LiveKitIdentity.fromUser(joinRequest.getUserId().get(), joinRequest.getDeviceId())
                    : LiveKitIdentity.forGuest(joinRequest.getDeviceId());

            var tokenResult = liveKitPort.generateToken(
                    roomName, identity, joinRequest.getDisplayName(), role);
            if (tokenResult instanceof Result.Failure<?, MeetingError>) {
                continue; // Skip on token generation failure
            }

            // Update status
            joinRequestRepository.updateStatus(
                    joinRequest.getId().value(), JoinRequestStatus.APPROVED);

            // Publish events
            var approvedEvent = new JoinRequestApprovedEvent(
                    UUID.randomUUID(),
                    command.meetingId(),
                    joinRequest.getId().value(),
                    command.approvedBy(),
                    Instant.now());
            applicationEventPublisher.publishEvent(approvedEvent);

            Map<String, Object> sseData = new HashMap<>();
            sseData.put("requestId", joinRequest.getId().value().toString());
            sseData.put("status", "APPROVED");
            sseData.put("approvedBy", command.approvedBy().toString());
            sseEventPublisher.publish(command.meetingId(), "join_request_approved", sseData);

            // Remove from queue
            joinRequestRepository.removeFromQueue(command.meetingId(), joinRequest.getId().value());

            approvedCount++;
        }

        return Result.success(new ApproveAllResponse(approvedCount));
    }
}
