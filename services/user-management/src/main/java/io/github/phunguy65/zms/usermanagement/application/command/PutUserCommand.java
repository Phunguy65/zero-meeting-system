package io.github.phunguy65.zms.usermanagement.application.command;

import jakarta.annotation.Nullable;

/**
 * Command for replacing a user's profile via PUT. All fields are required.
 *
 * <p>Unlike {@link PatchUserCommand}, this command expects all fields to be provided.
 * A {@code null} value for {@code avatarUrl} means clear the avatar.
 *
 * <p>Preferences are not included — use {@link PatchPreferencesCommand} for preference updates.
 */
public record PutUserCommand(
        String fullName, String username, @Nullable String avatarUrl) {}
