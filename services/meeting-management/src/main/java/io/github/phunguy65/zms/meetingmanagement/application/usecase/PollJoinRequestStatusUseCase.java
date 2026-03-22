package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.response.RequestJoinResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PollJoinRequestStatusUseCase {

    private final JoinRequestRepository joinRequestRepository;
    private final MeetingRepository meetingRepository;
    private final LiveKitPort liveKitPort;

    public PollJoinRequestStatusUseCase(
            JoinRequestRepository joinRequestRepository,
            MeetingRepository meetingRepository,
            LiveKitPort liveKitPort) {
        this.joinRequestRepository = joinRequestRepository;
        this.meetingRepository = meetingRepository;
        this.liveKitPort = liveKitPort;
    }

    @Transactional(readOnly = true)
    public Result<RequestJoinResponse, MeetingError> execute(UUID requestId) {
        var requestOpt = joinRequestRepository.findById(requestId);
        if (requestOpt.isEmpty()) {
            return Result.failure(new MeetingError.JoinRequestNotFound(null, requestId));
        }
        var joinRequest = requestOpt.get();

        // If APPROVED, generate a fresh token
        if (joinRequest.getStatus() == JoinRequestStatus.APPROVED) {
            var meetingOpt = meetingRepository.findById(joinRequest.getMeetingId().value());
            if (meetingOpt.isEmpty()) {
                return Result.failure(
                        new MeetingError.MeetingNotFound(joinRequest.getMeetingId().value()));
            }
            var meeting = meetingOpt.get();

            LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());
            ParticipantRole role = joinRequest.getUserId().isPresent()
                    ? ParticipantRole.PARTICIPANT
                    : ParticipantRole.GUEST;

            LiveKitIdentity identity = joinRequest.getUserId().isPresent()
                    ? LiveKitIdentity.fromUser(
                            joinRequest.getUserId().get(), joinRequest.getDeviceId())
                    : LiveKitIdentity.forGuest(joinRequest.getDeviceId());

            var tokenResult = liveKitPort.generateToken(
                    roomName, identity, joinRequest.getDisplayName(), role);
            if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
                return Result.failure(error);
            }
            String token = ((Result.Success<String, MeetingError>) tokenResult).value();

            return Result.success(new RequestJoinResponse(
                    joinRequest.getId().value(),
                    JoinRequestStatus.APPROVED,
                    token,
                    roomName.value()));
        }

        // For other statuses, return status-only response
        return Result.success(new RequestJoinResponse(
                joinRequest.getId().value(), joinRequest.getStatus(), null, null));
    }
}
