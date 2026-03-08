package io.github.phunguy65.zms.usermanagement.application.dto;

import jakarta.validation.constraints.Pattern;

/**
 * Typed representation of a user's stored preferences.
 */
public record UserPreferencesRequest(
        @Pattern(regexp = "dark|light|system", message = "theme must be dark, light, or system") String theme,

        boolean defaultMic,
        boolean defaultCamera) {

    /** Returns a {@code UserPreferencesRequest} with sensible defaults. */
    public static UserPreferencesRequest defaults() {
        return new UserPreferencesRequest("system", true, true);
    }
}
