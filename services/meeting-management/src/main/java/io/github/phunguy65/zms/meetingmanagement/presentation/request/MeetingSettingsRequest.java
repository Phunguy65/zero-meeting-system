package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.PutMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.model.AdmissionPolicy;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * API request DTO for meeting settings.
 *
 * <p>Carries client-supplied settings for creating, scheduling, or replacing a meeting,
 * including an optional plain-text password. Validation is enforced here at
 * the presentation boundary; business-rule validation (e.g. system ceiling on
 * participants) remains in the use-case layer.
 */
public record MeetingSettingsRequest(
        @NotBlank @Pattern(
                regexp = "ALLOW_ALL|MANUAL_APPROVAL",
                message = "admissionPolicy must be one of: ALLOW_ALL, MANUAL_APPROVAL")
        String admissionPolicy,

        @Nullable @Min(30) @Max(600) Integer joinRequestTimeoutSeconds,

        @NotNull Boolean allowGuest,
        @NotNull Boolean muteOnEntry,
        @NotNull @Min(1) @Max(1000) Integer maxParticipants,
        @NotNull Boolean recordingEnabled,

        @NotBlank @Pattern(
                regexp = MeetingSettings.SCREEN_SHARE_ALL
                        + "|"
                        + MeetingSettings.SCREEN_SHARE_HOST_ONLY
                        + "|"
                        + MeetingSettings.SCREEN_SHARE_DISABLED,
                message = "screenShareMode must be one of: ALL, HOST_ONLY, DISABLED")
        String screenShareMode,

        @NotNull Boolean chatEnabled,

        @Nullable @Size(max = 128) @Pattern(regexp = ".*\\S.*", message = "password must not be blank") String password) {

    /**
     * Maps this request to a domain {@link MeetingSettings} with no password hash.
     * The raw {@code password} is returned separately so the use-case layer can hash it.
     */
    public MeetingSettings toDomain() {
        return new MeetingSettings(
                AdmissionPolicy.valueOf(admissionPolicy),
                joinRequestTimeoutSeconds != null
                        ? Duration.ofSeconds(joinRequestTimeoutSeconds)
                        : null,
                allowGuest,
                muteOnEntry,
                maxParticipants,
                recordingEnabled,
                screenShareMode,
                chatEnabled,
                null);
    }

    public PutMeetingSettingsCommand toCommand(UUID meetingId, UUID requesterId) {
        return new PutMeetingSettingsCommand(meetingId, requesterId, toDomain(), password);
    }
}
