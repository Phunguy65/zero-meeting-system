package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.Theme;

/**
 * Repository interface for managing user session and app preferences.
 *
 * <p>This abstraction allows the domain and presentation layers to access
 * session/preferences without depending on concrete storage implementations.
 */
public interface SessionRepository {

    /**
     * Saves authentication tokens.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token
     */
    void saveTokens(String accessToken, String refreshToken);

    /**
     * Gets the current access token.
     *
     * @return the access token, or null if not stored
     */
    String getAccessToken();

    /**
     * Gets the current refresh token.
     *
     * @return the refresh token, or null if not stored
     */
    String getRefreshToken();

    /**
     * Clears all stored tokens.
     */
    void clearTokens();

    /**
     * Checks if tokens are currently stored.
     *
     * @return true if both access and refresh tokens exist
     */
    boolean hasTokens();

    /**
     * Saves user session information.
     *
     * @param sessionInfo the session info to save
     */
    void saveSession(SessionInfo sessionInfo);

    /**
     * Gets the stored session information.
     *
     * @return the session info, or null if not stored
     */
    SessionInfo getSession();

    /**
     * Clears stored session information.
     */
    void clearSession();

    /**
     * Sets the remember-me preference.
     *
     * @param rememberMe true to enable auto-login
     */
    void setRememberMe(boolean rememberMe);

    /**
     * Checks if remember-me is enabled.
     *
     * @return true if auto-login should be attempted
     */
    boolean isRememberMe();

    /**
     * Sets the app theme preference.
     *
     * @param theme the theme to set
     */
    void setTheme(Theme theme);

    /**
     * Gets the current theme preference.
     *
     * @return the theme, defaults to SYSTEM
     */
    Theme getTheme();

    /**
     * Sets the language preference.
     *
     * @param language the BCP 47 language tag (e.g., "en", "vi")
     */
    void setLanguage(String language);

    /**
     * Gets the current language preference.
     *
     * @return the language tag, defaults to "en"
     */
    String getLanguage();

    /**
     * Sets the last microphone enabled state.
     *
     * @param enabled whether mic was enabled
     */
    void setLastMicEnabled(boolean enabled);

    /**
     * Gets the last microphone enabled state.
     *
     * @return true if mic was last enabled, defaults to true
     */
    boolean getLastMicEnabled();

    /**
     * Sets the last camera enabled state.
     *
     * @param enabled whether camera was enabled
     */
    void setLastCameraEnabled(boolean enabled);

    /**
     * Gets the last camera enabled state.
     *
     * @return true if camera was last enabled, defaults to true
     */
    boolean getLastCameraEnabled();

    /**
     * Clears all session data (tokens, session info, remember-me flag).
     * Used during logout.
     */
    default void clearAllSessionData() {
        clearTokens();
        clearSession();
        setRememberMe(false);
    }
}
