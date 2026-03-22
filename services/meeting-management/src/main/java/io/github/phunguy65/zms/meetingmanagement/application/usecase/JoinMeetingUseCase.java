package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.JoinMeetingCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.JoinMeetingResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JoinMeetingUseCase {

    private final MeetingRepository meetingRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final LiveKitPort liveKitPort;
    private final PasswordHasher passwordHasher;

    public JoinMeetingUseCase(
            MeetingRepository meetingRepository,
            ParticipationLogRepository participationLogRepository,
            LiveKitPort liveKitPort,
            PasswordHasher passwordHasher) {
        this.meetingRepository = meetingRepository;
        this.participationLogRepository = participationLogRepository;
        this.liveKitPort = liveKitPort;
        this.passwordHasher = passwordHasher;
    }

    @Transactional
    public Result<JoinMeetingResponse, MeetingError> execute(JoinMeetingCommand command) {
        var meeting = meetingRepository.findByIdWithLock(command.meetingId());
        if (meeting.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var m = meeting.get();

        if (m.getStatus() != MeetingStatus.LIVE) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(m.getStatus(), MeetingStatus.LIVE));
        }

        boolean isHost =
                command.userId() != null && m.getHostId().equals(UserId.of(command.userId()));

//       TODO:
//        if (!isHost && m.getSettings().requiredApproval()) {
//        }

        if (command.userId() == null && !m.getSettings().allowGuest()) {
            return Result.failure(new MeetingError.GuestNotAllowed(m.getId().value()));
        }

        if (!isHost && m.getSettings().isPasswordProtected()) {
            String rawPassword = command.password();
            String storedHash = m.getSettings().passwordHash();
            if (rawPassword == null
                    || rawPassword.isBlank()
                    || !passwordHasher.verify(rawPassword, storedHash)) {
                return Result.failure(new MeetingError.InvalidPassword(m.getId().value()));
            }
        }

        ParticipantRole role;
        if (command.userId() == null) {
            role = ParticipantRole.GUEST;
        } else if (isHost) {
            role = ParticipantRole.HOST;
        } else {
            role = ParticipantRole.PARTICIPANT;
        }

        int maxParticipants = m.getSettings().maxParticipants();
        if (maxParticipants > 0 && role != ParticipantRole.HOST) {
            long activeCount =
                    participationLogRepository.countActiveByMeetingId(m.getId().value());
            if (activeCount >= maxParticipants) {
                return Result.failure(
                        new MeetingError.MeetingFull(m.getId().value(), maxParticipants));
            }
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(m.getId());

        LiveKitIdentity identity = command.userId() != null
                ? LiveKitIdentity.fromUser(UserId.of(command.userId()), command.deviceId())
                : LiveKitIdentity.forGuest(command.deviceId());

        var tokenResult =
                liveKitPort.generateToken(roomName, identity, command.displayName(), role);
        if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
            return Result.failure(error);
        }
        String token = ((Result.Success<String, MeetingError>) tokenResult).value();

        return Result.success(new JoinMeetingResponse(token, roomName.value()));
    }
}
