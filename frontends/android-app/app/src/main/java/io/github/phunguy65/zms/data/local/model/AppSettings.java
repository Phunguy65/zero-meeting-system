package io.github.phunguy65.zms.data.local.model;

/**
 * Represents the persisted app settings stored in SharedPreferences.
 *
 * <p>Contains user preferences for theme, language, and last-used meeting device states.
 *
 * @param theme the user's theme preference (DARK, LIGHT, or SYSTEM)
 * @param language the user's language preference (BCP 47 tag, e.g., "en", "vi")
 * @param lastMicEnabled whether the mic was enabled in the last meeting
 * @param lastCameraEnabled whether the camera was enabled in the last meeting
 */
public record AppSettings(
        ThemeMode theme, String language, boolean lastMicEnabled, boolean lastCameraEnabled) {

    /** Default language: English. */
    public static final String DEFAULT_LANGUAGE = "en";

    /** Default settings: System theme, English language, mic on, camera on. */
    public static final AppSettings DEFAULT =
            new AppSettings(ThemeMode.SYSTEM, DEFAULT_LANGUAGE, true, true);

    /**
     * Creates a copy with a new theme mode.
     *
     * @param newTheme the new theme mode
     * @return a new AppSettings with the updated theme
     */
    public AppSettings withTheme(ThemeMode newTheme) {
        return new AppSettings(newTheme, language, lastMicEnabled, lastCameraEnabled);
    }

    /**
     * Creates a copy with a new language.
     *
     * @param newLanguage the new language (BCP 47 tag)
     * @return a new AppSettings with the updated language
     */
    public AppSettings withLanguage(String newLanguage) {
        return new AppSettings(theme, newLanguage, lastMicEnabled, lastCameraEnabled);
    }

    /**
     * Creates a copy with updated mic state.
     *
     * @param enabled the new mic state
     * @return a new AppSettings with the updated mic state
     */
    public AppSettings withMic(boolean enabled) {
        return new AppSettings(theme, language, enabled, lastCameraEnabled);
    }

    /**
     * Creates a copy with updated camera state.
     *
     * @param enabled the new camera state
     * @return a new AppSettings with the updated camera state
     */
    public AppSettings withCamera(boolean enabled) {
        return new AppSettings(theme, language, lastMicEnabled, enabled);
    }
}
