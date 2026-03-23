package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.RequestJoinCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.RequestJoinResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestCreatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.*;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestJoinUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final ParticipationLogRepository participationLogRepository;
    private final LiveKitPort liveKitPort;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RequestJoinUseCase(
            MeetingRepository meetingRepository,
            JoinRequestRepository joinRequestRepository,
            ParticipationLogRepository participationLogRepository,
            LiveKitPort liveKitPort,
            PasswordHasher passwordHasher,
            ApplicationEventPublisher applicationEventPublisher) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.participationLogRepository = participationLogRepository;
        this.liveKitPort = liveKitPort;
        this.passwordHasher = passwordHasher;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public Result<RequestJoinResponse, MeetingError> execute(RequestJoinCommand command) {
        var meetingOpt = meetingRepository.findByIdWithLock(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        if (meeting.getStatus() != MeetingStatus.LIVE) {
            return Result.failure(new MeetingError.InvalidStatusTransition(
                    meeting.getStatus(), MeetingStatus.LIVE));
        }

        if (command.userId() == null && !meeting.getSettings().allowGuest()) {
            return Result.failure(
                    new MeetingError.GuestNotAllowed(meeting.getId().value()));
        }

        boolean isHost =
                command.userId() != null && meeting.getHostId().equals(UserId.of(command.userId()));
        if (!isHost && meeting.getSettings().isPasswordProtected()) {
            String rawPassword = command.password();
            if (rawPassword == null
                    || rawPassword.isBlank()
                    || !passwordHasher.verify(rawPassword, meeting.getSettings().passwordHash())) {
                return Result.failure(
                        new MeetingError.InvalidPassword(meeting.getId().value()));
            }
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());

        if (meeting.getSettings().admissionPolicy() == AdmissionPolicy.ALLOW_ALL) {
            ParticipantRole role = isHost
                    ? ParticipantRole.HOST
                    : (command.userId() == null
                            ? ParticipantRole.GUEST
                            : ParticipantRole.PARTICIPANT);

            int maxParticipants = meeting.getSettings().maxParticipants();
            if (maxParticipants > 0 && role != ParticipantRole.HOST) {
                long activeCount = participationLogRepository.countActiveByMeetingId(
                        meeting.getId().value());
                if (activeCount >= maxParticipants) {
                    return Result.failure(
                            new MeetingError.MeetingFull(meeting.getId().value(), maxParticipants));
                }
            }

            LiveKitIdentity identity = command.userId() != null
                    ? LiveKitIdentity.fromUser(UserId.of(command.userId()), command.deviceId())
                    : LiveKitIdentity.forGuest(command.deviceId());

            var tokenResult =
                    liveKitPort.generateToken(roomName, identity, command.displayName(), role);
            if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
                return Result.failure(error);
            }
            String token = ((Result.Success<String, MeetingError>) tokenResult).value();

            return Result.success(new RequestJoinResponse(
                    UUID.randomUUID(), JoinRequestStatus.APPROVED, token, roomName.value()));
        }

        Optional<JoinRequest> existing = findExisting(command, meeting.getId());
        if (existing.isPresent()) {
            JoinRequest req = existing.get();
            return Result.success(
                    new RequestJoinResponse(req.getId().value(), req.getStatus(), null, null));
        }

        Duration ttl = meeting.getSettings().joinRequestTimeout() != null
                ? meeting.getSettings().joinRequestTimeout()
                : Duration.ofMinutes(5);

        JoinRequest joinRequest = JoinRequest.create(
                meeting.getId(),
                command.userId() != null ? UserId.of(command.userId()) : null,
                command.displayName(),
                command.deviceId(),
                Instant.now().plus(ttl));

        joinRequestRepository.save(joinRequest, ttl);

        var createdEvent = new JoinRequestCreatedEvent(
                UUID.randomUUID(),
                meeting.getId().value(),
                joinRequest.getId().value(),
                command.userId(),
                command.displayName(),
                command.deviceId(),
                Instant.now());
        applicationEventPublisher.publishEvent(createdEvent);

        return Result.success(new RequestJoinResponse(
                joinRequest.getId().value(), JoinRequestStatus.PENDING, null, null));
    }

    private Optional<JoinRequest> findExisting(RequestJoinCommand command, MeetingId meetingId) {
        // Check by userId first (for authenticated users)
        if (command.userId() != null) {
            var byUser = joinRequestRepository.findPendingByMeetingId(meetingId.value()).stream()
                    .filter(r -> r.getUserId()
                            .map(uid -> uid.value().equals(command.userId()))
                            .orElse(false))
                    .filter(r -> r.getStatus() == JoinRequestStatus.PENDING)
                    .findFirst();
            if (byUser.isPresent()) return byUser;
        }

        // Check by deviceId (for guests and as fallback)
        return joinRequestRepository
                .findByDeviceId(meetingId.value(), command.deviceId())
                .filter(r -> r.getStatus() == JoinRequestStatus.PENDING);
    }
}
