package io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject;

import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.shared.domain.ValueObject;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * Meeting room settings stored as JSONB in the database.
 *
 * <p>Mapped via {@code @JdbcTypeCode(SqlTypes.JSON)} on the JPA entity.
 * This is a pure domain value object — it carries no Jackson or persistence
 * annotations. Serialization concerns are handled by dedicated DTOs:
 * <ul>
 *   <li>{@code MeetingSettingsJson} — persistence layer (JSONB)</li>
 *   <li>{@code MeetingSettingsRequest} — API input</li>
 *   <li>{@code MeetingSettingsResponse} — API output</li>
 * </ul>
 */
public record MeetingSettings(
        AdmissionPolicy admissionPolicy,
        @Nullable Duration joinRequestTimeout,
        boolean allowGuest,
        boolean muteOnEntry,
        int maxParticipants,
        boolean recordingEnabled,
        String screenShareMode,
        boolean chatEnabled,
        @Nullable String passwordHash)
        implements ValueObject {

    /** Allowed values for {@code screenShareMode}. */
    public static final String SCREEN_SHARE_ALL = "ALL";

    public static final String SCREEN_SHARE_HOST_ONLY = "HOST_ONLY";
    public static final String SCREEN_SHARE_DISABLED = "DISABLED";

    /** Returns {@code true} if this meeting requires a password to join. */
    public boolean isPasswordProtected() {
        return passwordHash != null && !passwordHash.isBlank();
    }

    /** Default settings (no password). */
    public static MeetingSettings defaults() {
        return new MeetingSettings(
                AdmissionPolicy.MANUAL_APPROVAL,
                Duration.ofMinutes(5),
                true,
                false,
                100,
                false,
                SCREEN_SHARE_ALL,
                true,
                null);
    }
}
