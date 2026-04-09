package io.github.phunguy65.zms.meetingmanagement.application.command;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Command for patching meeting settings.
 *
 * <p>Each {@link JsonNullable} field follows the standard patch semantics:
 * <ul>
 *   <li>{@code JsonNullable.undefined()} — field absent from request, keep existing value</li>
 *   <li>{@code JsonNullable.of(value)} — update to this value</li>
 *   <li>{@code JsonNullable.of(null)} — clear the field (only valid for nullable fields)</li>
 * </ul>
 */
public record UpdateMeetingSettingsCommand(
        UUID meetingId,
        UUID requesterId,
        JsonNullable<String> admissionPolicy,
        JsonNullable<Boolean> allowGuest,
        JsonNullable<Boolean> muteOnEntry,
        JsonNullable<Integer> maxParticipants,
        JsonNullable<Boolean> recordingEnabled,
        JsonNullable<String> screenShareMode,
        JsonNullable<Boolean> chatEnabled,
        @Nullable JsonNullable<Integer> joinRequestTimeoutSeconds) {}
