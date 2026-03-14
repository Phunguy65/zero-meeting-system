package io.github.phunguy65.zms.meetingmanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.event.RecordingCompletedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.RecordingFailedEvent;
import io.github.phunguy65.zms.meetingmanagement.domain.event.RecordingStartedEvent;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import io.github.phunguy65.zms.shared.domain.Result;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Recording aggregate root — separate from Meeting to allow independent lifecycle management.
 *
 * <p>A recording is created when a meeting goes LIVE and transitions through:
 * RECORDING → PROCESSING → COMPLETED (or FAILED at any point).
 *
 * <p>The {@code file_url} and {@code thumbnail_url} are populated asynchronously by the
 * MinIO/S3 callback once the file is uploaded.
 */
public class Recording extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID meetingId;
    private final Instant startedAt;
    private final Instant createdAt;

    private @Nullable String fileUrl;
    private @Nullable String thumbnailUrl;
    private RecordingStatus status;
    private @Nullable Instant endedAt;
    private int durationSeconds;
    private long fileSizeBytes;

    // -------------------------------------------------------------------------
    // Private constructor
    // -------------------------------------------------------------------------

    private Recording(
            UUID id,
            UUID meetingId,
            @Nullable String fileUrl,
            @Nullable String thumbnailUrl,
            RecordingStatus status,
            Instant startedAt,
            @Nullable Instant endedAt,
            int durationSeconds,
            long fileSizeBytes,
            Instant createdAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.fileSizeBytes = fileSizeBytes;
        this.createdAt = createdAt;
    }

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /** Creates a new recording for a meeting that just went LIVE. */
    public static Recording startFor(UUID meetingId) {
        UUID id = UuidCreator.getTimeOrderedEpoch();
        Instant now = Instant.now();
        Recording recording = new Recording(
                id, meetingId, null, null, RecordingStatus.RECORDING, now, null, 0, 0L, now);
        recording.registerEvent(new RecordingStartedEvent(UUID.randomUUID(), id, meetingId, now));
        return recording;
    }

    /** Reconstitutes a Recording from persistence. No domain events registered. */
    public static Recording reconstitute(
            UUID id,
            UUID meetingId,
            @Nullable String fileUrl,
            @Nullable String thumbnailUrl,
            RecordingStatus status,
            Instant startedAt,
            @Nullable Instant endedAt,
            int durationSeconds,
            long fileSizeBytes,
            Instant createdAt) {
        return new Recording(
                id,
                meetingId,
                fileUrl,
                thumbnailUrl,
                status,
                startedAt,
                endedAt,
                durationSeconds,
                fileSizeBytes,
                createdAt);
    }

    // -------------------------------------------------------------------------
    // Domain behaviours
    // -------------------------------------------------------------------------

    /** Transitions RECORDING → PROCESSING (file upload started). */
    public Result<Void, MeetingError> markProcessing() {
        if (!status.canTransitionTo(RecordingStatus.PROCESSING)) {
            return Result.failure(new MeetingError.InvalidRecordingTransition(
                    status, RecordingStatus.PROCESSING));
        }
        status = RecordingStatus.PROCESSING;
        endedAt = Instant.now();
        return Result.success();
    }

    /** Transitions PROCESSING → COMPLETED. Called by MinIO/S3 callback. */
    public Result<Void, MeetingError> complete(
            String fileUrl,
            @Nullable String thumbnailUrl,
            int durationSeconds,
            long fileSizeBytes) {
        if (!status.canTransitionTo(RecordingStatus.COMPLETED)) {
            return Result.failure(
                    new MeetingError.InvalidRecordingTransition(status, RecordingStatus.COMPLETED));
        }
        this.status = RecordingStatus.COMPLETED;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.durationSeconds = durationSeconds;
        this.fileSizeBytes = fileSizeBytes;
        registerEvent(new RecordingCompletedEvent(
                UUID.randomUUID(),
                id,
                meetingId,
                fileUrl,
                durationSeconds,
                fileSizeBytes,
                Instant.now()));
        return Result.success();
    }

    /** Transitions RECORDING or PROCESSING → FAILED. */
    public Result<Void, MeetingError> fail() {
        if (!status.canTransitionTo(RecordingStatus.FAILED)) {
            return Result.failure(
                    new MeetingError.InvalidRecordingTransition(status, RecordingStatus.FAILED));
        }
        status = RecordingStatus.FAILED;
        if (endedAt == null) endedAt = Instant.now();
        registerEvent(new RecordingFailedEvent(UUID.randomUUID(), id, meetingId, Instant.now()));
        return Result.success();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getMeetingId() {
        return meetingId;
    }

    public @Nullable String getFileUrl() {
        return fileUrl;
    }

    public @Nullable String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public RecordingStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public @Nullable Instant getEndedAt() {
        return endedAt;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
