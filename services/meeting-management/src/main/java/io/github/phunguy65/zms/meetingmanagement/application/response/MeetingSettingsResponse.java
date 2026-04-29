package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;

/**
 * API response DTO for meeting settings.
 *
 * <p>Exposes only the fields that are safe to return to clients.
 * The stored password/hash value from the domain object is intentionally
 * excluded — callers never need to see it.
 */
public record MeetingSettingsResponse(
        String admissionPolicy,
        boolean allowGuest,
        int maxParticipants,
        boolean allowScreenShare,
        boolean chatEnabled,
        boolean allowMicrophone,
        boolean allowVideo,
        boolean requirePassword) {

    /**
     * Builds a response from the domain value object.
     */
    public static MeetingSettingsResponse from(MeetingSettings settings) {
        return new MeetingSettingsResponse(
                settings.admissionPolicy().name(),
                settings.allowGuest(),
                settings.maxParticipants(),
                settings.allowScreenShare(),
                settings.chatEnabled(),
                settings.allowMicrophone(),
                settings.allowVideo(),
                settings.isPasswordProtected());
    }
}
