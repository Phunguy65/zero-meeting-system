package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import org.jspecify.annotations.Nullable;

/**
 * API response DTO for meeting settings.
 *
 * <p>Exposes only the fields that are safe to return to clients.
 * The {@code passwordHash} field from the domain object is intentionally
 * excluded — callers never need to see it.
 */
public record MeetingSettingsResponse(
        String admissionPolicy,
        @Nullable Integer joinRequestTimeoutSeconds,
        boolean allowGuest,
        boolean muteOnEntry,
        int maxParticipants,
        boolean recordingEnabled,
        String screenShareMode,
        boolean chatEnabled,
        boolean requirePassword) {

    /**
     * Builds a response from the domain value object.
     */
    public static MeetingSettingsResponse from(MeetingSettings settings) {
        return new MeetingSettingsResponse(
                settings.admissionPolicy().name(),
                settings.joinRequestTimeout() != null
                        ? (int) settings.joinRequestTimeout().toSeconds()
                        : null,
                settings.allowGuest(),
                settings.muteOnEntry(),
                settings.maxParticipants(),
                settings.recordingEnabled(),
                settings.screenShareMode(),
                settings.chatEnabled(),
                settings.isPasswordProtected());
    }
}
