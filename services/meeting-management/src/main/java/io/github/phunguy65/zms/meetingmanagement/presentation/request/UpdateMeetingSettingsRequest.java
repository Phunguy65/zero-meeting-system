package io.github.phunguy65.zms.meetingmanagement.presentation.request;

import io.github.phunguy65.zms.meetingmanagement.application.command.UpdateMeetingSettingsCommand;
import io.github.phunguy65.zms.meetingmanagement.domain.model.valueobject.MeetingSettings;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * API request DTO for patching meeting settings.
 *
 * <p>Uses {@link JsonNullable} to distinguish between:
 * <ul>
 *   <li>Field absent from request body → {@code JsonNullable.undefined()} → keep existing value</li>
 *   <li>Field present with a value → {@code JsonNullable.of(value)} → update to new value</li>
 *   <li>Field present as JSON {@code null} → {@code JsonNullable.of(null)} → clear the value
 *       (only valid for {@code joinRequestTimeoutSeconds})</li>
 * </ul>
 *
 * <p>Must be a class (not a record) to allow Jackson default-constructor deserialization
 * with {@link JsonNullable} fields initialised to {@code undefined()}.
 *
 * <p>Requires {@code JsonNullableJackson3Module} to be registered — provided by
 * {@code shared} module's {@code JacksonConfig}.
 */
public class UpdateMeetingSettingsRequest {

    private JsonNullable<
                    @Pattern(
                            regexp = "ALLOW_ALL|MANUAL_APPROVAL",
                            message = "admissionPolicy must be one of: ALLOW_ALL, MANUAL_APPROVAL")
                    String>
            admissionPolicy = JsonNullable.undefined();

    private JsonNullable<Boolean> allowGuest = JsonNullable.undefined();

    private JsonNullable<Boolean> muteOnEntry = JsonNullable.undefined();

    private JsonNullable<@Min(1) @Max(1000) Integer> maxParticipants = JsonNullable.undefined();

    private JsonNullable<Boolean> recordingEnabled = JsonNullable.undefined();

    private JsonNullable<
                    @Pattern(
                            regexp = MeetingSettings.SCREEN_SHARE_ALL
                                    + "|"
                                    + MeetingSettings.SCREEN_SHARE_HOST_ONLY
                                    + "|"
                                    + MeetingSettings.SCREEN_SHARE_DISABLED,
                            message = "screenShareMode must be one of: ALL, HOST_ONLY, DISABLED")
                    String>
            screenShareMode = JsonNullable.undefined();

    private JsonNullable<Boolean> chatEnabled = JsonNullable.undefined();

    /**
     * {@code null} value clears the timeout (no expiry).
     */
    private JsonNullable<@Min(30) @Max(600) Integer> joinRequestTimeoutSeconds =
            JsonNullable.undefined();

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    public UpdateMeetingSettingsCommand toCommand(UUID meetingId, UUID requesterId) {
        return new UpdateMeetingSettingsCommand(
                meetingId,
                requesterId,
                admissionPolicy,
                allowGuest,
                muteOnEntry,
                maxParticipants,
                recordingEnabled,
                screenShareMode,
                chatEnabled,
                joinRequestTimeoutSeconds);
    }

    // -------------------------------------------------------------------------
    // Getters / setters (required by Jackson)
    // -------------------------------------------------------------------------

    public JsonNullable<String> getAdmissionPolicy() {
        return admissionPolicy;
    }

    public void setAdmissionPolicy(JsonNullable<String> admissionPolicy) {
        this.admissionPolicy = admissionPolicy;
    }

    public JsonNullable<Boolean> getAllowGuest() {
        return allowGuest;
    }

    public void setAllowGuest(JsonNullable<Boolean> allowGuest) {
        this.allowGuest = allowGuest;
    }

    public JsonNullable<Boolean> getMuteOnEntry() {
        return muteOnEntry;
    }

    public void setMuteOnEntry(JsonNullable<Boolean> muteOnEntry) {
        this.muteOnEntry = muteOnEntry;
    }

    public JsonNullable<Integer> getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(JsonNullable<Integer> maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public JsonNullable<Boolean> getRecordingEnabled() {
        return recordingEnabled;
    }

    public void setRecordingEnabled(JsonNullable<Boolean> recordingEnabled) {
        this.recordingEnabled = recordingEnabled;
    }

    public JsonNullable<String> getScreenShareMode() {
        return screenShareMode;
    }

    public void setScreenShareMode(JsonNullable<String> screenShareMode) {
        this.screenShareMode = screenShareMode;
    }

    public JsonNullable<Boolean> getChatEnabled() {
        return chatEnabled;
    }

    public void setChatEnabled(JsonNullable<Boolean> chatEnabled) {
        this.chatEnabled = chatEnabled;
    }

    public JsonNullable<Integer> getJoinRequestTimeoutSeconds() {
        return joinRequestTimeoutSeconds;
    }

    public void setJoinRequestTimeoutSeconds(JsonNullable<Integer> joinRequestTimeoutSeconds) {
        this.joinRequestTimeoutSeconds = joinRequestTimeoutSeconds;
    }
}
