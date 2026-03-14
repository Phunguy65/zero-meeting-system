package io.github.phunguy65.zms.usermanagement.presentation.request;

import io.github.phunguy65.zms.usermanagement.application.command.PatchUserCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * PATCH request DTO for updating a user's profile fields. All fields default to
 * {@link JsonNullable#undefined()} so absent fields are ignored during processing.
 *
 * <p>If {@code preferences} is present, the entire stored preferences object is replaced
 * (RFC 7386 JSON Merge Patch semantics). Use {@code PATCH /users/me/preferences} for
 * granular per-field preference updates.
 */
public record PatchUserRequest(
        @Size(max = 255) @NotBlank JsonNullable<String> fullName,
        @Size(max = 2048) JsonNullable<String> avatarUrl,

        @Size(min = 3, max = 30) @NotBlank @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Username must contain only letters, digits, _ or -")
        JsonNullable<String> username,

        JsonNullable<Map<String, Object>> preferences) {

    public PatchUserCommand toCommand() {
        return new PatchUserCommand(fullName, avatarUrl, username, preferences);
    }
}
