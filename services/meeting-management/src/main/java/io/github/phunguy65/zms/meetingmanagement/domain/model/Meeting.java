package io.github.phunguy65.zms.meetingmanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingCancelledEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingEndedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingScheduledEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.MeetingStartedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitRoomName;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import io.github.phunguy65.zms.shared.domain.Result;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Meeting aggregate root.
 *
 * <p>Manages the meeting lifecycle: SCHEDULED → LIVE → ENDED, or SCHEDULED → CANCELLED.
 * Domain events are registered on each state transition and published by the infrastructure layer
 * via the Transactional Outbox pattern.
 */
public class Meeting extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID hostId;
    private final ShortCode shortCode;
    private final MeetingType type;
    private final Instant createdAt;

    private @Nullable String title;
    private @Nullable Instant startTime;
    private @Nullable Instant endTime;
    private MeetingStatus status;
    private MeetingSettings settings;

    // -------------------------------------------------------------------------
    // Private constructor — use factory methods
    // -------------------------------------------------------------------------

    private Meeting(
            UUID id,
            UUID hostId,
            ShortCode shortCode,
            @Nullable String title,
            @Nullable Instant startTime,
            @Nullable Instant endTime,
            MeetingType type,
            MeetingStatus status,
            MeetingSettings settings,
            Instant createdAt) {
        this.id = id;
        this.hostId = hostId;
        this.shortCode = shortCode;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.type = type;
        this.status = status;
        this.settings = settings;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a new SCHEDULED meeting. Registers {@code MeetingScheduledEvent}.
     */
    public static Meeting schedule(
            UUID hostId,
            @Nullable String title,
            Instant startTime,
            Instant endTime,
            MeetingSettings settings,
            ShortCode shortCode) {
        UUID id = UuidCreator.getTimeOrderedEpoch();
        Instant now = Instant.now();
        Meeting meeting = new Meeting(
                id,
                hostId,
                shortCode,
                title,
                startTime,
                endTime,
                MeetingType.SCHEDULED,
                MeetingStatus.SCHEDULED,
                settings,
                now);
        meeting.registerEvent(new MeetingScheduledEvent(
                UUID.randomUUID(), id, hostId, shortCode.value(), title, startTime, now));
        return meeting;
    }

    /**
     * Creates a new INSTANT meeting (starts immediately, no scheduled time).
     * Registers {@code MeetingScheduledEvent}.
     */
    public static Meeting instant(
            UUID hostId, @Nullable String title, MeetingSettings settings, ShortCode shortCode) {
        UUID id = UuidCreator.getTimeOrderedEpoch();
        Instant now = Instant.now();
        Meeting meeting = new Meeting(
                id,
                hostId,
                shortCode,
                title,
                null,
                null,
                MeetingType.INSTANT,
                MeetingStatus.SCHEDULED,
                settings,
                now);
        meeting.registerEvent(new MeetingScheduledEvent(
                UUID.randomUUID(), id, hostId, shortCode.value(), title, null, now));
        return meeting;
    }

    /**
     * Reconstitutes a Meeting from persistence. No domain events are registered.
     */
    public static Meeting reconstitute(
            UUID id,
            UUID hostId,
            ShortCode shortCode,
            @Nullable String title,
            @Nullable Instant startTime,
            @Nullable Instant endTime,
            MeetingType type,
            MeetingStatus status,
            MeetingSettings settings,
            Instant createdAt) {
        return new Meeting(
                id, hostId, shortCode, title, startTime, endTime, type, status, settings,
                createdAt);
    }

    // -------------------------------------------------------------------------
    // Domain behaviours
    // -------------------------------------------------------------------------

    /**
     * Transitions SCHEDULED → LIVE. Registers {@code MeetingStartedEvent}.
     */
    public Result<Void, MeetingError> start() {
        if (!status.canTransitionTo(MeetingStatus.LIVE)) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(status, MeetingStatus.LIVE));
        }
        status = MeetingStatus.LIVE;
        Instant now = Instant.now();
        registerEvent(new MeetingStartedEvent(
                UUID.randomUUID(), id, hostId, LiveKitRoomName.fromMeetingId(id).value(), now));
        return Result.success();
    }

    /**
     * Transitions LIVE → ENDED. Registers {@code MeetingEndedEvent}.
     */
    public Result<Void, MeetingError> end() {
        if (!status.canTransitionTo(MeetingStatus.ENDED)) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(status, MeetingStatus.ENDED));
        }
        status = MeetingStatus.ENDED;
        Instant now = Instant.now();
        this.endTime = now;
        registerEvent(new MeetingEndedEvent(UUID.randomUUID(), id, hostId, now));
        return Result.success();
    }

    /**
     * Transitions SCHEDULED → CANCELLED. Registers {@code MeetingCancelledEvent}.
     */
    public Result<Void, MeetingError> cancel() {
        if (!status.canTransitionTo(MeetingStatus.CANCELLED)) {
            return Result.failure(
                    new MeetingError.InvalidStatusTransition(status, MeetingStatus.CANCELLED));
        }
        status = MeetingStatus.CANCELLED;
        Instant now = Instant.now();
        registerEvent(new MeetingCancelledEvent(UUID.randomUUID(), id, hostId, now));
        return Result.success();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getHostId() {
        return hostId;
    }

    public ShortCode getShortCode() {
        return shortCode;
    }

    public @Nullable String getTitle() {
        return title;
    }

    public @Nullable Instant getStartTime() {
        return startTime;
    }

    public @Nullable Instant getEndTime() {
        return endTime;
    }

    public MeetingType getType() {
        return type;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public MeetingSettings getSettings() {
        return settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
