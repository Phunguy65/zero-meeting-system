package io.github.phunguy65.zms.meetingmanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.InviteeAcceptedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.InviteeDeclinedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviteeDisplayName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviteeId;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.InviterId;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Represents a pre-scheduled invitation for a meeting.
 *
 * <p>Created at scheduling time when the host provides an invitee list.
 * {@code userId} is populated from gRPC resolution; {@code email} is the stable invite key.
 *
 * <p>Status transitions: {@code PENDING → ACCEPTED}, {@code PENDING → DECLINED},
 * {@code ACCEPTED → DECLINED}.
 */
public class MeetingInvitee extends AggregateRoot<InviteeId> {

    private final InviteeId id;
    private final MeetingId meetingId;
    private final InviterId inviterId;
    private @Nullable UserId userId;
    private final Email email;
    private final @Nullable InviteeDisplayName displayName;
    private InviteeStatus status;
    private final Instant invitedAt;
    private @Nullable Instant respondedAt;

    private MeetingInvitee(
            InviteeId id,
            MeetingId meetingId,
            InviterId inviterId,
            @Nullable UserId userId,
            Email email,
            @Nullable InviteeDisplayName displayName,
            InviteeStatus status,
            Instant invitedAt,
            @Nullable Instant respondedAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.inviterId = inviterId;
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.invitedAt = invitedAt;
        this.respondedAt = respondedAt;
    }

    /** Factory method — creates a new PENDING invitation. */
    public static MeetingInvitee create(
            MeetingId meetingId,
            InviterId inviterId,
            @Nullable UserId userId,
            Email email,
            @Nullable InviteeDisplayName displayName) {
        return new MeetingInvitee(
                InviteeId.of(UuidCreator.getTimeOrderedEpoch()),
                meetingId,
                inviterId,
                userId,
                email,
                displayName,
                InviteeStatus.PENDING,
                Instant.now(),
                null);
    }

    /** Reconstitution factory used by the persistence adapter. */
    public static MeetingInvitee reconstitute(
            InviteeId id,
            MeetingId meetingId,
            InviterId inviterId,
            @Nullable UserId userId,
            Email email,
            @Nullable InviteeDisplayName displayName,
            InviteeStatus status,
            Instant invitedAt,
            @Nullable Instant respondedAt) {
        return new MeetingInvitee(
                id,
                meetingId,
                inviterId,
                userId,
                email,
                displayName,
                status,
                invitedAt,
                respondedAt);
    }

    /**
     * Accepts the invitation.
     *
     * @return {@code Result.success()} on success, or {@code Result.failure(InvalidInviteeTransition)}
     *         if the current status does not allow transitioning to ACCEPTED
     */
    public Result<Void, MeetingError> accept() {
        if (!status.canTransitionTo(InviteeStatus.ACCEPTED)) {
            return Result.failure(
                    new MeetingError.InvalidInviteeTransition(status, InviteeStatus.ACCEPTED));
        }
        status = InviteeStatus.ACCEPTED;
        respondedAt = Instant.now();
        registerEvent(new InviteeAcceptedEvent(
                UUID.randomUUID(), id.value(), meetingId.value(), inviterId.value(), respondedAt));
        return Result.success();
    }

    /**
     * Declines the invitation.
     *
     * @return {@code Result.success()} on success, or {@code Result.failure(InvalidInviteeTransition)}
     *         if the current status does not allow transitioning to DECLINED
     */
    public Result<Void, MeetingError> decline() {
        if (!status.canTransitionTo(InviteeStatus.DECLINED)) {
            return Result.failure(
                    new MeetingError.InvalidInviteeTransition(status, InviteeStatus.DECLINED));
        }
        status = InviteeStatus.DECLINED;
        respondedAt = Instant.now();
        registerEvent(new InviteeDeclinedEvent(
                UUID.randomUUID(), id.value(), meetingId.value(), inviterId.value(), respondedAt));
        return Result.success();
    }

    @Override
    public InviteeId getId() {
        return id;
    }

    public MeetingId getMeetingId() {
        return meetingId;
    }

    public InviterId getInviterId() {
        return inviterId;
    }

    public Optional<UserId> getUserId() {
        return Optional.ofNullable(userId);
    }

    public Email getEmail() {
        return email;
    }

    public Optional<InviteeDisplayName> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public InviteeStatus getStatus() {
        return status;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public Optional<Instant> getRespondedAt() {
        return Optional.ofNullable(respondedAt);
    }
}
