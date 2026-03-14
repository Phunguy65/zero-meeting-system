package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.Recording;
import io.github.phunguy65.zms.meetingmanagement.domain.model.RecordingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.port.RecordingRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RecordingRepositoryAdapter implements RecordingRepository {

    private final RecordingJpaRepository jpa;

    public RecordingRepositoryAdapter(RecordingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Recording save(Recording recording) {
        RecordingJpaEntity entity = toEntity(recording);
        jpa.save(entity);
        return recording;
    }

    @Override
    public Optional<Recording> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Recording> findActiveByMeetingId(UUID meetingId) {
        return jpa.findByMeetingIdAndStatusIn(
                        meetingId, List.of(RecordingStatus.RECORDING, RecordingStatus.PROCESSING))
                .map(this::toDomain);
    }

    @Override
    public List<Recording> findByMeetingId(UUID meetingId) {
        return jpa.findByMeetingId(meetingId).stream().map(this::toDomain).toList();
    }

    private Recording toDomain(RecordingJpaEntity e) {
        return Recording.reconstitute(
                e.getId(),
                e.getMeetingId(),
                e.getFileUrl(),
                e.getThumbnailUrl(),
                e.getStatus(),
                e.getStartedAt(),
                e.getEndedAt(),
                e.getDurationSeconds(),
                e.getFileSizeBytes(),
                e.getCreatedAt());
    }

    private RecordingJpaEntity toEntity(Recording r) {
        return new RecordingJpaEntity(
                r.getId(),
                r.getMeetingId(),
                r.getFileUrl(),
                r.getThumbnailUrl(),
                r.getStatus(),
                r.getStartedAt(),
                r.getEndedAt(),
                r.getDurationSeconds(),
                r.getFileSizeBytes(),
                r.getCreatedAt());
    }
}
