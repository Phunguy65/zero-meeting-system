package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ParticipationLogRepositoryAdapter implements ParticipationLogRepository {

    private final ParticipationLogJpaRepository jpa;

    public ParticipationLogRepositoryAdapter(ParticipationLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public ParticipationLog save(ParticipationLog log) {
        ParticipationLogJpaEntity entity;
        if (log.getId() != null) {
            // Update existing row (e.g., recording left_at)
            entity = jpa.findById(log.getId()).orElseThrow();
            if (log.getLeftAt() != null) {
                entity.setLeftAt(log.getLeftAt());
            }
        } else {
            entity = toEntity(log);
        }
        ParticipationLogJpaEntity saved = jpa.save(entity);
        if (log.getId() == null) {
            log.assignId(saved.getId());
        }
        return log;
    }

    @Override
    public Optional<ParticipationLog> findActiveByMeetingIdAndDeviceId(
            UUID meetingId, String deviceId) {
        return jpa.findActiveByMeetingIdAndDeviceId(meetingId, deviceId).map(this::toDomain);
    }

    @Override
    public List<ParticipationLog> findByMeetingId(UUID meetingId) {
        return jpa.findByMeetingId(meetingId).stream().map(this::toDomain).toList();
    }

    private ParticipationLog toDomain(ParticipationLogJpaEntity e) {
        return ParticipationLog.reconstitute(
                e.getId(),
                e.getMeetingId(),
                e.getUserId(),
                e.getDisplayName(),
                e.getRole(),
                e.getJoinedAt(),
                e.getLeftAt(),
                e.getDeviceId());
    }

    private ParticipationLogJpaEntity toEntity(ParticipationLog log) {
        return new ParticipationLogJpaEntity(
                log.getMeetingId(),
                log.getUserId(),
                log.getDisplayName(),
                log.getRole(),
                log.getJoinedAt(),
                log.getLeftAt(),
                log.getDeviceId());
    }
}
