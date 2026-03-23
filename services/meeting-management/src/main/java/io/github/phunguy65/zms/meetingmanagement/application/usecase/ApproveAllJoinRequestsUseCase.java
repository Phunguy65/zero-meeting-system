package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveAllJoinRequestsCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.ApproveAllResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestApprovedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveAllJoinRequestsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApproveAllJoinRequestsUseCase.class);

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final LiveKitPort liveKitPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApproveAllJoinRequestsUseCase(
            MeetingRepository meetingRepository,
            JoinRequestRepository joinRequestRepository,
            ParticipationLogRepository participationLogRepository,
            LiveKitPort liveKitPort,
            ApplicationEventPublisher applicationEventPublisher) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.participationLogRepository = participationLogRepository;
        this.liveKitPort = liveKitPort;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Result<ApproveAllResponse, MeetingError> execute(ApproveAllJoinRequestsCommand command) {
        var meetingOpt = meetingRepository.findByIdWithLock(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        if (!meeting.getHostId().equals(UserId.of(command.approvedBy()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.approvedBy(), meeting.getHostId().value()));
        }

        List<JoinRequest> pendingRequests =
                joinRequestRepository.findPendingByMeetingId(command.meetingId());

        if (pendingRequests.isEmpty()) {
            return Result.success(new ApproveAllResponse(0));
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());
        int approvedCount = 0;

        int maxParticipants = meeting.getSettings().maxParticipants();

        long remainingSlots = maxParticipants > 0
                ? maxParticipants
                        - participationLogRepository.countActiveByMeetingId(
                                meeting.getId().value())
                : Long.MAX_VALUE;

        for (JoinRequest joinRequest : pendingRequests) {
            if (remainingSlots <= 0) {
                continue;
            }

            var approveResult = joinRequest.approve();
            if (approveResult instanceof Result.Failure<?, MeetingError>) {
                continue;
            }

            ParticipantRole role = joinRequest.getUserId().isPresent()
                    ? ParticipantRole.PARTICIPANT
                    : ParticipantRole.GUEST;

            LiveKitIdentity identity = joinRequest.getUserId().isPresent()
                    ? LiveKitIdentity.fromUser(
                            joinRequest.getUserId().get(), joinRequest.getDeviceId())
                    : LiveKitIdentity.forGuest(joinRequest.getDeviceId());

            var tokenResult = liveKitPort.generateToken(
                    roomName, identity, joinRequest.getDisplayName(), role);
            if (tokenResult instanceof Result.Failure<?, MeetingError>) {
                continue;
            }
            String token = ((Result.Success<String, MeetingError>) tokenResult).value();

            joinRequestRepository.updateStatus(
                    joinRequest.getId().value(), JoinRequestStatus.APPROVED);

            ParticipationLog participationLog = ParticipationLog.join(
                    meeting.getId(),
                    joinRequest.getUserId().map(UserId::value).orElse(null),
                    joinRequest.getDisplayName(),
                    role,
                    identity);
            participationLogRepository.save(participationLog);
            log.debug(
                    "Recorded participation log for identity '{}' in meeting '{}'",
                    identity.value(),
                    meeting.getId().value());

            var approvedEvent = new JoinRequestApprovedEvent(
                    UUID.randomUUID(),
                    command.meetingId(),
                    joinRequest.getId().value(),
                    command.approvedBy(),
                    token,
                    Instant.now());
            applicationEventPublisher.publishEvent(approvedEvent);

            joinRequestRepository.removeFromQueue(
                    command.meetingId(), joinRequest.getId().value());

            approvedCount++;
            remainingSlots--;
        }

        return Result.success(new ApproveAllResponse(approvedCount));
    }
}
