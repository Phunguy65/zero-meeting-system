package io.github.phunguy65.zms.meetingmanagement.infrastructure.persistence;

import org.jspecify.annotations.Nullable;

/**
 * Persistence DTO for {@code MeetingSettings} stored as JSONB.
 *
 * <p>This is a pure data holder used exclusively by the persistence layer.
 * It carries no domain logic and no API serialization concerns.
 * Mapped via {@code @JdbcTypeCode(SqlTypes.JSON)} on {@link MeetingJpaEntity}.
 */
public record MeetingSettingsJson(
        String admissionPolicy,
        @Nullable Integer joinRequestTimeoutSeconds,
        boolean allowGuest,
        boolean muteOnEntry,
        int maxParticipants,
        boolean recordingEnabled,
        String screenShareMode,
        boolean chatEnabled,
        @Nullable String passwordHash) {}
