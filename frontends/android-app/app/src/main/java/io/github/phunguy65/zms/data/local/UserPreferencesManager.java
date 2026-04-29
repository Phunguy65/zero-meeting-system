package io.github.phunguy65.zms.data.local;

import android.content.SharedPreferences;
import io.github.phunguy65.zms.data.local.model.AppSettings;
import io.github.phunguy65.zms.data.local.model.ThemeMode;
import io.github.phunguy65.zms.data.local.model.UserSession;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Manages user preferences persistence using SharedPreferences.
 *
 * <p>Handles two categories of data:
 * <ul>
 *   <li><b>User Session</b>: Profile data (userId, email, etc.) and rememberMe flag</li>
 *   <li><b>App Settings</b>: Theme preference and last mic/camera states</li>
 * </ul>
 *
 * <p>Note: Unlike tokens in {@link TokenManager}, this data is not sensitive and
 * doesn't require encryption. It uses standard SharedPreferences for simplicity.
 */
@Singleton
public class UserPreferencesManager {

    // User session keys
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_AVATAR_URL = "avatar_url";
    private static final String KEY_REMEMBER_ME = "remember_me";

    // App settings keys
    private static final String KEY_THEME = "theme";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LAST_MIC_ENABLED = "last_mic_enabled";
    private static final String KEY_LAST_CAMERA_ENABLED = "last_camera_enabled";

    private final SharedPreferences prefs;

    @Inject
    public UserPreferencesManager(@Named("userPrefs") SharedPreferences prefs) {
        this.prefs = prefs;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // User Session Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Saves the user session data.
     *
     * @param session the user session to persist
     */
    public void saveUserSession(UserSession session) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_USER_ID, session.userId());
        editor.putString(KEY_EMAIL, session.email());
        editor.putString(KEY_FULL_NAME, session.fullName());
        editor.putString(KEY_USERNAME, session.username());
        if (session.avatarUrl() != null) {
            editor.putString(KEY_AVATAR_URL, session.avatarUrl());
        } else {
            editor.remove(KEY_AVATAR_URL);
        }
        editor.putBoolean(KEY_REMEMBER_ME, session.rememberMe());
        editor.apply();
    }

    /**
     * Gets the current user session.
     *
     * @return the current UserSession or null if no session exists
     */
    public UserSession getUserSession() {
        String userId = prefs.getString(KEY_USER_ID, null);
        if (userId == null) {
            return null;
        }
        return new UserSession(
                userId,
                prefs.getString(KEY_EMAIL, null),
                prefs.getString(KEY_FULL_NAME, null),
                prefs.getString(KEY_USERNAME, null),
                prefs.getString(KEY_AVATAR_URL, null),
                prefs.getBoolean(KEY_REMEMBER_ME, false));
    }

    /**
     * Clears all user session data.
     */
    public void clearSession() {
        prefs.edit()
                .remove(KEY_USER_ID)
                .remove(KEY_EMAIL)
                .remove(KEY_FULL_NAME)
                .remove(KEY_USERNAME)
                .remove(KEY_AVATAR_URL)
                .remove(KEY_REMEMBER_ME)
                .apply();
    }

    /**
     * Sets the rememberMe flag.
     *
     * @param rememberMe whether to remember the session
     */
    public void setRememberMe(boolean rememberMe) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, rememberMe).apply();
    }

    /**
     * Gets the current rememberMe flag.
     *
     * @return true if rememberMe is enabled, false otherwise
     */
    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // App Settings Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Sets the theme mode preference.
     *
     * @param themeMode the theme mode to set
     */
    public void setThemeMode(ThemeMode themeMode) {
        prefs.edit().putString(KEY_THEME, themeMode.name()).apply();
    }

    /**
     * Gets the current theme mode.
     *
     * @return the current ThemeMode, defaulting to SYSTEM
     */
    public ThemeMode getThemeMode() {
        String themeStr = prefs.getString(KEY_THEME, null);
        return ThemeMode.fromString(themeStr);
    }

    /**
     * Sets the language preference.
     *
     * @param language the BCP 47 language tag (e.g., "en", "vi")
     */
    public void setLanguage(String language) {
        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    /**
     * Gets the current language preference.
     *
     * @return the current language tag, defaulting to "en"
     */
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, AppSettings.DEFAULT_LANGUAGE);
    }

    /**
     * Sets the last mic enabled state.
     *
     * @param enabled whether the mic was enabled
     */
    public void setLastMicEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LAST_MIC_ENABLED, enabled).apply();
    }

    /**
     * Gets the last mic enabled state.
     *
     * @return the last mic state, defaulting to true
     */
    public boolean getLastMicEnabled() {
        return prefs.getBoolean(KEY_LAST_MIC_ENABLED, true);
    }

    /**
     * Sets the last camera enabled state.
     *
     * @param enabled whether the camera was enabled
     */
    public void setLastCameraEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_LAST_CAMERA_ENABLED, enabled).apply();
    }

    /**
     * Gets the last camera enabled state.
     *
     * @return the last camera state, defaulting to true
     */
    public boolean getLastCameraEnabled() {
        return prefs.getBoolean(KEY_LAST_CAMERA_ENABLED, true);
    }

    /**
     * Gets the current app settings.
     *
     * @return the current AppSettings
     */
    public AppSettings getAppSettings() {
        return new AppSettings(
                getThemeMode(), getLanguage(), getLastMicEnabled(), getLastCameraEnabled());
    }
}
