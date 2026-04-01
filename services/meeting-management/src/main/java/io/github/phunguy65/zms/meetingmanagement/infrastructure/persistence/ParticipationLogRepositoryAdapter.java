package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipantRole;
import io.github.phunguy65.zms.meetingmanagement.domain.model.ParticipationLog;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitIdentity;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.LiveKitParticipantSid;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogCursor;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipationLogId;
import io.github.phunguy65.zms.meetingmanagement.domain.port.ParticipationLogRepository;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
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
            entity = jpa.findById(log.getId().value()).orElseThrow();
            log.getLeftAt().ifPresent(entity::setLeftAt);
            log.getLivekitParticipantSid()
                    .map(LiveKitParticipantSid::value)
                    .ifPresent(entity::setLivekitParticipantSid);
        } else {
            entity = toEntity(log);
        }
        ParticipationLogJpaEntity saved = jpa.save(entity);
        if (log.getId() == null) {
            log.assignId(ParticipationLogId.of(saved.getId()));
        }
        return log;
    }

    @Override
    public Optional<ParticipationLog> findActiveBySid(LiveKitParticipantSid sid) {
        return jpa.findActiveBySid(sid.value()).map(this::toDomain);
    }

    @Override
    public Optional<ParticipationLog> findActiveByMeetingIdAndIdentity(
            UUID meetingId, LiveKitIdentity identity) {
        return jpa.findActiveByMeetingIdAndIdentity(meetingId, identity.value())
                .map(this::toDomain);
    }

    @Override
    public List<ParticipationLog> findByMeetingId(UUID meetingId) {
        return jpa.findByMeetingId(meetingId).stream().map(this::toDomain).toList();
    }

    @Override
    public long countActiveByMeetingId(UUID meetingId) {
        return jpa.countActiveByMeetingId(meetingId);
    }

    @Override
    public List<ParticipationLog> findActiveByMeetingId(UUID meetingId) {
        return jpa.findActiveByMeetingId(meetingId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<ParticipationLog> findActiveByUserId(UUID userId) {
        return jpa.findActiveByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public CursorPageResponse<ParticipationLog> findByMeetingIdKeyset(
            UUID meetingId, ParticipationLogCursor cursor, int pageSize) {
        int fetchLimit = pageSize + 1;
        var cursorJoinedAt = cursor != null ? cursor.joinedAt() : null;
        var cursorId = cursor != null ? cursor.id().value() : null;

        List<ParticipationLogJpaEntity> rows = jpa.findByMeetingIdKeyset(
                meetingId.toString(), cursorJoinedAt, cursorId, fetchLimit);

        boolean hasNext = rows.size() > pageSize;
        List<ParticipationLog> items =
                rows.stream().limit(pageSize).map(this::toDomain).toList();
        return CursorPageResponse.of(items, pageSize, hasNext);
    }

    private ParticipationLog toDomain(ParticipationLogJpaEntity e) {
        return ParticipationLog.reconstitute(
                ParticipationLogId.of(e.getId()),
                MeetingId.of(e.getMeetingId()),
                e.getUserId(),
                e.getDisplayName(),
                ParticipantRole.valueOf(e.getRole()),
                LiveKitIdentity.of(e.getLivekitIdentity()),
                e.getLivekitParticipantSid() != null
                        ? LiveKitParticipantSid.of(e.getLivekitParticipantSid())
                        : null,
                e.getJoinedAt(),
                e.getLeftAt());
    }

    private ParticipationLogJpaEntity toEntity(ParticipationLog log) {
        return new ParticipationLogJpaEntity(
                log.getMeetingId().value(),
                log.getUserId().orElse(null),
                log.getDisplayName(),
                log.getRole().name(),
                log.getLivekitIdentity().value(),
                log.getLivekitParticipantSid().map(LiveKitParticipantSid::value).orElse(null),
                log.getJoinedAt(),
                log.getLeftAt().orElse(null));
    }
}
