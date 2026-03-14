package io.github.phunguy65.zms.meetingmanagement.domain.model;

import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Participation log aggregate — append-only event log of join/leave events.
 *
 * <p>Each row represents one participation session (one device joining once).
 * A participant rejoining creates a new row. {@code user_id} is null for guest participants.
 */
public class ParticipationLog extends AggregateRoot<Long> {

    private Long id; // bigserial — assigned by DB on insert
    private final UUID meetingId;
    private final @Nullable UUID userId; // null = guest
    private final String displayName;
    private final ParticipantRole role;
    private final Instant joinedAt;
    private final @Nullable String deviceId;

    private @Nullable Instant leftAt;

    // -------------------------------------------------------------------------
    // Private constructor
    // -------------------------------------------------------------------------

    private ParticipationLog(
            @Nullable Long id,
            UUID meetingId,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role,
            Instant joinedAt,
            @Nullable Instant leftAt,
            @Nullable String deviceId) {
        this.id = id;
        this.meetingId = meetingId;
        this.userId = userId;
        this.displayName = displayName;
        this.role = role;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
        this.deviceId = deviceId;
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Records a participant joining a meeting. */
    public static ParticipationLog join(
            UUID meetingId,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role,
            @Nullable String deviceId) {
        return new ParticipationLog(
                null, meetingId, userId, displayName, role, Instant.now(), null, deviceId);
    }

    /** Reconstitutes from persistence. */
    public static ParticipationLog reconstitute(
            Long id,
            UUID meetingId,
            @Nullable UUID userId,
            String displayName,
            ParticipantRole role,
            Instant joinedAt,
            @Nullable Instant leftAt,
            @Nullable String deviceId) {
        return new ParticipationLog(
                id, meetingId, userId, displayName, role, joinedAt, leftAt, deviceId);
    }

    // -------------------------------------------------------------------------
    // Domain behaviours
    // -------------------------------------------------------------------------

    /** Records the time the participant left. Can only be called once. */
    public void leave(Instant leftAt) {
        if (this.leftAt != null) {
            throw new IllegalStateException("Participant already recorded as left");
        }
        this.leftAt = leftAt;
    }

    /** Returns true if this is a guest participant (no registered user account). */
    public boolean isGuest() {
        return userId == null;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @Override
    public Long getId() {
        return id;
    }

    /** Called by the persistence adapter after insert to set the DB-generated id. */
    public void assignId(Long id) {
        if (this.id != null) throw new IllegalStateException("Id already assigned");
        this.id = id;
    }

    public UUID getMeetingId() {
        return meetingId;
    }

    public @Nullable UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public @Nullable Instant getLeftAt() {
        return leftAt;
    }

    public @Nullable String getDeviceId() {
        return deviceId;
    }
}
