package io.github.phunguy65.zms.usermanagement.application.command;

import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;

/**
 * Command for patching a user's profile fields. All fields default to
 * {@link JsonNullable#undefined()} so absent fields are ignored during processing.
 *
 * <p>If {@code preferences} is present, the entire stored preferences object is replaced
 * (RFC 7386 JSON Merge Patch semantics).
 */
public record PatchUserCommand(
        JsonNullable<String> fullName,
        JsonNullable<String> avatarUrl,
        JsonNullable<String> username,
        JsonNullable<Map<String, Object>> preferences) {}
