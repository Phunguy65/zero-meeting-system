package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTimeRange;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.domain.ScrollParams;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Duration;
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
    public Optional<Meeting> findByIdWithLock(UUID id) {
        return jpa.findByIdWithLock(id).map(this::toDomain);
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
        MeetingTitle title = e.getTitle() != null ? MeetingTitle.of(e.getTitle()) : null;
        MeetingTimeRange timeRange = e.getStartTime() != null && e.getEndTime() != null
                ? MeetingTimeRange.of(e.getStartTime(), e.getEndTime())
                : null;
        java.time.Instant endTime = (timeRange == null) ? e.getEndTime() : null;
        MeetingSettings settings = new MeetingSettings(
                AdmissionPolicy.valueOf(e.getSettings().admissionPolicy()),
                e.getSettings().joinRequestTimeoutSeconds() != null
                        ? Duration.ofSeconds(e.getSettings().joinRequestTimeoutSeconds())
                        : null,
                e.getSettings().allowGuest(),
                e.getSettings().muteOnEntry(),
                e.getSettings().maxParticipants(),
                e.getSettings().recordingEnabled(),
                e.getSettings().screenShareMode(),
                e.getSettings().chatEnabled(),
                e.getSettings().passwordHash());
        return Meeting.reconstitute(
                MeetingId.of(e.getId()),
                UserId.of(e.getHostId()),
                ShortCode.of(e.getShortCode()),
                title,
                e.getDescription(),
                timeRange,
                endTime,
                MeetingType.valueOf(e.getType()),
                MeetingStatus.valueOf(e.getStatus()),
                settings,
                e.getCreatedAt());
    }

    private MeetingJpaEntity toEntity(Meeting m) {
        return new MeetingJpaEntity(
                m.getId().value(),
                m.getHostId().value(),
                m.getShortCode().value(),
                m.getTitle().map(MeetingTitle::value).orElse(null),
                m.getDescription().orElse(null),
                m.getStartTime().orElse(null),
                m.getEndTime().orElse(null),
                m.getType().name(),
                m.getStatus().name(),
                new MeetingSettingsJson(
                        m.getSettings().admissionPolicy().name(),
                        m.getSettings().joinRequestTimeout() != null
                                ? (int) m.getSettings().joinRequestTimeout().toSeconds()
                                : null,
                        m.getSettings().allowGuest(),
                        m.getSettings().muteOnEntry(),
                        m.getSettings().maxParticipants(),
                        m.getSettings().recordingEnabled(),
                        m.getSettings().screenShareMode(),
                        m.getSettings().chatEnabled(),
                        m.getSettings().passwordHash()),
                m.getCreatedAt());
    }
}
