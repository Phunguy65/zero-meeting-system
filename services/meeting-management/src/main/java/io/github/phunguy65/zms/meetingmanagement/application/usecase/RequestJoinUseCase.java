package io.github.phunguy65.zms.meetingmanagement.application.usecase;

import io.github.phunguy65.zms.meetingmanagement.application.command.RequestJoinCommand;
import io.github.phunguy65.zms.meetingmanagement.application.response.RequestJoinResponse;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.JoinRequestCreatedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequest;
import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.port.JoinRequestRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.LiveKitPort;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.meetingmanagement.infrastructure.sse.RedisSseEventPublisher;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RequestJoinUseCase {

    private final MeetingRepository meetingRepository;
    private final JoinRequestRepository joinRequestRepository;
    private final LiveKitPort liveKitPort;
    private final PasswordHasher passwordHasher;
    private final RedisSseEventPublisher sseEventPublisher;
    private final ApplicationEventPublisher applicationEventPublisher;

    public RequestJoinUseCase(
            MeetingRepository meetingRepository,
            JoinRequestRepository joinRequestRepository,
            LiveKitPort liveKitPort,
            PasswordHasher passwordHasher,
            RedisSseEventPublisher sseEventPublisher,
            ApplicationEventPublisher applicationEventPublisher) {
        this.meetingRepository = meetingRepository;
        this.joinRequestRepository = joinRequestRepository;
        this.liveKitPort = liveKitPort;
        this.passwordHasher = passwordHasher;
        this.sseEventPublisher = sseEventPublisher;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional(readOnly = true)
    public Result<RequestJoinResponse, MeetingError> execute(RequestJoinCommand command) {
        var meetingOpt = meetingRepository.findByIdWithLock(command.meetingId());
        if (meetingOpt.isEmpty()) {
            return Result.failure(new MeetingError.MeetingNotFound(command.meetingId()));
        }
        var meeting = meetingOpt.get();

        // Meeting must be live
        if (meeting.getStatus() != MeetingStatus.LIVE) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(meeting.getStatus(), MeetingStatus.LIVE));
        }

        // Guest check
        if (command.userId() == null && !meeting.getSettings().allowGuest()) {
            return Result.failure(new MeetingError.GuestNotAllowed(meeting.getId().value()));
        }

        // Password check (hosts bypass)
        boolean isHost = command.userId() != null
                && meeting.getHostId().equals(UserId.of(command.userId()));
        if (!isHost && meeting.getSettings().isPasswordProtected()) {
            String rawPassword = command.password();
            if (rawPassword == null
                    || rawPassword.isBlank()
                    || !passwordHasher.verify(rawPassword, meeting.getSettings().passwordHash())) {
                return Result.failure(new MeetingError.InvalidPassword(meeting.getId().value()));
            }
        }

        LiveKitRoomName roomName = LiveKitRoomName.fromMeetingId(meeting.getId());

        // ALLOW_ALL — generate token immediately
        if (meeting.getSettings().admissionPolicy() == AdmissionPolicy.ALLOW_ALL) {
            ParticipantRole role = isHost ? ParticipantRole.HOST
                    : (command.userId() == null ? ParticipantRole.GUEST : ParticipantRole.PARTICIPANT);

            LiveKitIdentity identity = command.userId() != null
                    ? LiveKitIdentity.fromUser(UserId.of(command.userId()), command.deviceId())
                    : LiveKitIdentity.forGuest(command.deviceId());

            var tokenResult = liveKitPort.generateToken(
                    roomName, identity, command.displayName(), role);
            if (tokenResult instanceof Result.Failure<?, MeetingError>(MeetingError error)) {
                return Result.failure(error);
            }
            String token = ((Result.Success<String, MeetingError>) tokenResult).value();

            return Result.success(new RequestJoinResponse(
                    UUID.randomUUID(), JoinRequestStatus.APPROVED, token, roomName.value()));
        }

        // MANUAL_APPROVAL — check for duplicate, then create request
        Optional<JoinRequest> existing = findExisting(command, meeting.getId());
        if (existing.isPresent()) {
            JoinRequest req = existing.get();
            return Result.success(new RequestJoinResponse(
                    req.getId().value(), req.getStatus(), null, null));
        }

        // Determine TTL
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

        // Publish domain event (Kafka outbox)
        var createdEvent = new JoinRequestCreatedEvent(
                UUID.randomUUID(),
                meeting.getId().value(),
                joinRequest.getId().value(),
                command.userId(),
                command.displayName(),
                command.deviceId(),
                Instant.now());
        applicationEventPublisher.publishEvent(createdEvent);

        // Publish SSE event (Redis Pub/Sub → host)
        Map<String, Object> sseData = new HashMap<>();
        sseData.put("requestId", joinRequest.getId().value().toString());
        sseData.put("displayName", command.displayName());
        sseData.put("userId", command.userId() != null ? command.userId().toString() : null);
        sseData.put("deviceId", command.deviceId());
        sseData.put("requestedAt", joinRequest.getRequestedAt().toString());
        sseData.put("expiresAt", joinRequest.getExpiresAt().toString());
        sseEventPublisher.publish(meeting.getId().value(), "join_request_created", sseData);

        return Result.success(new RequestJoinResponse(
                joinRequest.getId().value(), JoinRequestStatus.PENDING, null, null));
    }

    private Optional<JoinRequest> findExisting(RequestJoinCommand command, MeetingId meetingId) {
        // Check by userId first (for authenticated users)
        if (command.userId() != null) {
            var byUser = joinRequestRepository.findPendingByMeetingId(meetingId.value())
                    .stream()
                    .filter(r -> r.getUserId()
                            .map(uid -> uid.value().equals(command.userId()))
                            .orElse(false))
                    .filter(r -> r.getStatus() == JoinRequestStatus.PENDING)
                    .findFirst();
            if (byUser.isPresent()) return byUser;
        }

        // Check by deviceId (for guests and as fallback)
        return joinRequestRepository.findByDeviceId(meetingId.value(), command.deviceId())
                .filter(r -> r.getStatus() == JoinRequestStatus.PENDING);
    }
}
