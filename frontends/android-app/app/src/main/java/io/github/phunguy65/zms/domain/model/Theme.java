package io.github.phunguy65.zms.domain.model;

/**
 * Represents the app's theme mode preference.
 *
 * <p>This is a domain-layer enum that is independent of Android framework classes.
 * The presentation layer is responsible for mapping this to AppCompatDelegate constants.
 */
public enum Theme {
    DARK,
    LIGHT,
    SYSTEM;

    /**
     * Parses a theme from its string representation.
     *
     * @param value the string value (case-insensitive)
     * @return the matching Theme, or SYSTEM if not found
     */
    public static Theme fromString(String value) {
        if (value == null) {
            return SYSTEM;
        }
        try {
            return Theme.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SYSTEM;
        }
    }
}
