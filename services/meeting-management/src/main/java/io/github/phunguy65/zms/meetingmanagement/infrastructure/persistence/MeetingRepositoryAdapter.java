package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import io.github.phunguy65.zms.meetingmanagement.domain.MeetingError;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.Meeting;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.model.MeetingType;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTimeRange;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingTitle;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ParticipatedMeetingCursor;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.ShortCode;
import io.github.phunguy65.zms.meetingmanagement.domain.port.MeetingRepository;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.MeetingSettingsSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.MeetingSummary;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.ParticipatedMeetingSummary;
import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.domain.valueobject.MeetingId;
import io.github.phunguy65.zms.shared.domain.valueobject.UserId;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Repository
public class MeetingRepositoryAdapter implements MeetingRepository {

    private final MeetingJpaRepository jpa;
    private final ObjectMapper objectMapper;

    public MeetingRepositoryAdapter(MeetingJpaRepository jpa, ObjectMapper objectMapper) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
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
    public CursorPageResponse<MeetingSummary> findSummariesByHostId(
            UUID hostId, @Nullable ScrollCursor cursor, int pageSize) {
        int fetchLimit = pageSize + 1;
        var cursorCreatedAt = cursor != null ? cursor.createdAt() : null;
        var cursorId = cursor != null ? cursor.id().toString() : null;

        List<MeetingJpaEntity> rows =
                jpa.findByHostIdKeyset(hostId.toString(), cursorCreatedAt, cursorId, fetchLimit);

        boolean hasNext = rows.size() > pageSize;
        List<MeetingSummary> items =
                rows.stream().limit(pageSize).map(this::toSummary).toList();
        return CursorPageResponse.of(items, pageSize, hasNext);
    }

    @Override
    public CursorPageResponse<ParticipatedMeetingSummary> findParticipatedSummariesByUserId(
            UUID userId,
            Set<MeetingStatus> statuses,
            @Nullable ParticipatedMeetingCursor cursor,
            int pageSize) {
        int fetchLimit = pageSize + 1;
        var cursorJoinedAt = cursor != null ? cursor.lastJoinedAt() : null;
        var cursorMeetingId = cursor != null ? cursor.meetingId().toString() : null;

        List<ParticipatedMeetingRow> rows = statuses.isEmpty()
                ? jpa.findParticipatedMeetingsKeyset(
                        userId.toString(), cursorJoinedAt, cursorMeetingId, fetchLimit)
                : jpa.findParticipatedMeetingsKeysetByStatuses(
                        userId.toString(),
                        statuses.stream().map(MeetingStatus::name).toList(),
                        cursorJoinedAt,
                        cursorMeetingId,
                        fetchLimit);

        boolean hasNext = rows.size() > pageSize;
        List<ParticipatedMeetingSummary> items =
                rows.stream().limit(pageSize).map(this::toParticipatedSummary).toList();
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

    private MeetingSummary toSummary(MeetingJpaEntity e) {
        return new MeetingSummary(
                e.getId(),
                e.getHostId(),
                e.getShortCode(),
                e.getTitle(),
                e.getDescription(),
                e.getStartTime(),
                e.getEndTime(),
                MeetingType.valueOf(e.getType()),
                MeetingStatus.valueOf(e.getStatus()),
                new MeetingSettingsSummary(
                        e.getSettings().admissionPolicy(),
                        e.getSettings().joinRequestTimeoutSeconds(),
                        e.getSettings().allowGuest(),
                        e.getSettings().muteOnEntry(),
                        e.getSettings().maxParticipants(),
                        e.getSettings().recordingEnabled(),
                        e.getSettings().screenShareMode(),
                        e.getSettings().chatEnabled(),
                        e.getSettings().passwordHash() != null),
                e.getCreatedAt());
    }

    private ParticipatedMeetingSummary toParticipatedSummary(ParticipatedMeetingRow row) {
        MeetingSettingsJson settings;
        try {
            settings = objectMapper.readValue(row.getSettings(), MeetingSettingsJson.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    new MeetingError.InvalidSettings("Failed to decode meeting settings JSON")
                            .message(),
                    e);
        }

        return new ParticipatedMeetingSummary(
                row.getId(),
                row.getHostId(),
                row.getShortCode(),
                row.getTitle(),
                row.getDescription(),
                row.getStartTime(),
                row.getEndTime(),
                MeetingType.valueOf(row.getType()),
                MeetingStatus.valueOf(row.getStatus()),
                new MeetingSettingsSummary(
                        settings.admissionPolicy(),
                        settings.joinRequestTimeoutSeconds(),
                        settings.allowGuest(),
                        settings.muteOnEntry(),
                        settings.maxParticipants(),
                        settings.recordingEnabled(),
                        settings.screenShareMode(),
                        settings.chatEnabled(),
                        settings.passwordHash() != null),
                row.getCreatedAt(),
                row.getLastJoinedAt());
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
