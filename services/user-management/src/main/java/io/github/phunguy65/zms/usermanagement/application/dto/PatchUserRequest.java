package io.github.phunguy65.zms.usermanagement.application.dto;

import jakarta.validation.constraints.NotBlank;
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
        JsonNullable<@Size(max = 255) @NotBlank String> fullName,
        JsonNullable<@Size(max = 2048) String> avatarUrl,
        JsonNullable<Map<String, Object>> preferences) {

    public PatchUserRequest {
        if (fullName == null) fullName = JsonNullable.undefined();
        if (avatarUrl == null) avatarUrl = JsonNullable.undefined();
        if (preferences == null) preferences = JsonNullable.undefined();
    }

    public PatchUserRequest() {
        this(JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());
    }
}
