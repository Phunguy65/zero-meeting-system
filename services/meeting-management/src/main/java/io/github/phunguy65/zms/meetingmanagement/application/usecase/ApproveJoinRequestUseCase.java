package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.ApproveJoinRequestCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestApprovedEvent;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApproveJoinRequestUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApproveJoinRequestUseCase.class);

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final LiveKitPort liveKitPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    public ApproveJoinRequestUseCase(
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
    public Result<String, MeetingError> execute(ApproveJoinRequestCommand command) {
        var meetingOpt = meetingRepository.findByIdWithLock(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        if (!meeting.getHostId().equals(UserId.of(command.approvedBy()))) {
            return Result.failure(new MeetingError.NotAuthorized(
                    command.approvedBy(), meeting.getHostId().value()));
        }

        var requestOpt = joinRequestRepository.findById(command.requestId());
        if (requestOpt.isEmpty()) {
            return Result.failure(
                    new MeetingError.JoinRequestNotFound(command.meetingId(), command.requestId()));
        }
        var joinRequest = requestOpt.get();

        int maxParticipants = meeting.getSettings().maxParticipants();
        if (maxParticipants > 0) {
            long activeCount = participationLogRepository.countActiveByMeetingId(
                    meeting.getId().value());
            if (activeCount >= maxParticipants) {
                return Result.failure(
                        new MeetingError.MeetingFull(meeting.getId().value(), maxParticipants));
            }
        }

        var approveResult = joinRequest.approve();
        if (approveResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());
        ParticipantRole role = joinRequest.getUserId().isPresent()
                ? ParticipantRole.PARTICIPANT
                : ParticipantRole.GUEST;

        LiveKitIdentity identity = joinRequest.getUserId().isPresent()
                ? LiveKitIdentity.fromUser(joinRequest.getUserId().get(), joinRequest.getDeviceId())
                : LiveKitIdentity.forGuest(joinRequest.getDeviceId());

        var tokenResult =
                liveKitPort.generateToken(roomName, identity, joinRequest.getDisplayName(), role);
        if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }
        String token = ((Result.Success<String, MeetingError>) tokenResult).value();

        joinRequestRepository.updateStatus(command.requestId(), JoinRequestStatus.APPROVED);

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
                command.requestId(),
                command.approvedBy(),
                token,
                Instant.now());
        applicationEventPublisher.publishEvent(approvedEvent);

        joinRequestRepository.removeFromQueue(command.meetingId(), command.requestId());

        return Result.success(token);
    }
}
