package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.application.response.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.domain.ScrollParams;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingRepositoryAdapter implements MeetingRepository {

    private final MeetingJpaRepository jpa;

    public MeetingRepositoryAdapter(MeetingJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Meeting save(Meeting meeting) {
        MeetingJpaEntity entity = toEntity(meeting);
        jpa.save(entity);
        return meeting;
    }

    @Override
    public Optional<Meeting> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Meeting> findByShortCode(ShortCode shortCode) {
        return jpa.findByShortCode(shortCode.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByShortCode(ShortCode shortCode) {
        return jpa.existsByShortCode(shortCode.value());
    }

    @Override
    public CursorPageResponse<Meeting> findByHostId(UUID hostId, ScrollParams params) {
        int fetchLimit = params.pageSize() + 1;

        ScrollCursor cursor = params.pageToken()
                .flatMap(token -> {
                    // Cursor decoding is handled by the application layer via CursorTokenEncoder.
                    // The adapter receives a pre-decoded cursor via a dedicated overload if needed.
                    // For now, null cursor = first page.
                    return Optional.<ScrollCursor>empty();
                })
                .orElse(null);

        var cursorCreatedAt = cursor != null ? cursor.createdAt() : null;
        var cursorId = cursor != null ? cursor.id().toString() : null;

        List<MeetingJpaEntity> rows =
                jpa.findByHostIdKeyset(hostId.toString(), cursorCreatedAt, cursorId, fetchLimit);

        boolean hasNext = rows.size() > params.pageSize();
        List<Meeting> items =
                rows.stream().limit(params.pageSize()).map(this::toDomain).toList();

        return CursorPageResponse.of(items, params.pageSize(), hasNext);
    }

    /** Overload accepting a pre-decoded cursor — used by application layer use cases. */
    public CursorPageResponse<Meeting> findByHostId(
            UUID hostId, ScrollCursor cursor, int pageSize) {
        int fetchLimit = pageSize + 1;
        var cursorCreatedAt = cursor != null ? cursor.createdAt() : null;
        var cursorId = cursor != null ? cursor.id().toString() : null;

        List<MeetingJpaEntity> rows =
                jpa.findByHostIdKeyset(hostId.toString(), cursorCreatedAt, cursorId, fetchLimit);

        boolean hasNext = rows.size() > pageSize;
        List<Meeting> items = rows.stream().limit(pageSize).map(this::toDomain).toList();
        return CursorPageResponse.of(items, pageSize, hasNext);
    }

    private Meeting toDomain(MeetingJpaEntity e) {
        return Meeting.reconstitute(
                e.getId(),
                e.getHostId(),
                ShortCode.of(e.getShortCode()),
                e.getTitle(),
                e.getStartTime(),
                e.getEndTime(),
                e.getType(),
                e.getStatus(),
                e.getSettings(),
                e.getCreatedAt());
    }

    private MeetingJpaEntity toEntity(Meeting m) {
        return new MeetingJpaEntity(
                m.getId(),
                m.getHostId(),
                m.getShortCode().value(),
                m.getTitle(),
                m.getStartTime(),
                m.getEndTime(),
                m.getType(),
                m.getStatus(),
                m.getSettings(),
                m.getCreatedAt());
    }
}
