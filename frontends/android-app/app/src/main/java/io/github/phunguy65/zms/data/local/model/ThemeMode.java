package io.github.phunguy65.zms.data.local.model;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * Represents the user's theme preference.
 *
 * <p>Maps to Android's night mode constants for easy application via
 * {@link AppCompatDelegate#setDefaultNightMode(int)}.
 */
public enum ThemeMode {
    /** Always use dark theme. */
    DARK(AppCompatDelegate.MODE_NIGHT_YES),

    /** Always use light theme. */
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),

    /** Follow system theme setting. */
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

    private final int nightMode;

    ThemeMode(int nightMode) {
        this.nightMode = nightMode;
    }

    /**
     * Returns the corresponding {@link AppCompatDelegate} night mode constant.
     *
     * @return one of {@code MODE_NIGHT_YES}, {@code MODE_NIGHT_NO}, or
     *         {@code MODE_NIGHT_FOLLOW_SYSTEM}
     */
    public int toNightMode() {
        return nightMode;
    }

    /**
     * Converts a string value to a ThemeMode enum.
     *
     * @param value the string value (case-insensitive)
     * @return the corresponding ThemeMode, or {@code SYSTEM} if not recognized
     */
    public static ThemeMode fromString(String value) {
        if (value == null) {
            return SYSTEM;
        }
        return switch (value.toUpperCase()) {
            case "DARK" -> DARK;
            case "LIGHT" -> LIGHT;
            default -> SYSTEM;
        };
    }
}
