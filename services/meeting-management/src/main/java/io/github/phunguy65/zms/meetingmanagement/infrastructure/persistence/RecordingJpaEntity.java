package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.RecordingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "recordings")
public class RecordingJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "meeting_id", nullable = false, columnDefinition = "uuid")
    private UUID meetingId;

    @Column(name = "file_url", length = 2048)
    private @Nullable String fileUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private @Nullable String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordingStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private @Nullable Instant endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecordingJpaEntity() {}

    public RecordingJpaEntity(
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
