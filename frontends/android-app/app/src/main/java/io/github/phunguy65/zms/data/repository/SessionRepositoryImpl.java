package io.github.phunguy65.zms.data.repository;

import io.github.phunguy65.zms.data.local.TokenManager;
import io.github.phunguy65.zms.data.local.UserPreferencesManager;
import io.github.phunguy65.zms.data.local.model.ThemeMode;
import io.github.phunguy65.zms.data.local.model.UserSession;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.Theme;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link SessionRepository} that delegates to
 * {@link TokenManager} and {@link UserPreferencesManager}.
 *
 * <p>This class acts as a facade, combining the two local storage managers
 * behind a single domain-layer interface. This allows ViewModels to depend
 * on the domain abstraction rather than concrete data-layer classes.
 */
@Singleton
public class SessionRepositoryImpl implements SessionRepository {

    private final TokenManager tokenManager;
    private final UserPreferencesManager prefsManager;

    @Inject
    public SessionRepositoryImpl(TokenManager tokenManager, UserPreferencesManager prefsManager) {
        this.tokenManager = tokenManager;
        this.prefsManager = prefsManager;
    }

    @Override
    public void saveTokens(String accessToken, String refreshToken) {
        tokenManager.saveTokens(accessToken, refreshToken);
    }

    @Override
    public String getAccessToken() {
        return tokenManager.getAccessToken();
    }

    @Override
    public String getRefreshToken() {
        return tokenManager.getRefreshToken();
    }

    @Override
    public void clearTokens() {
        tokenManager.clearTokens();
    }

    @Override
    public boolean hasTokens() {
        return tokenManager.hasTokens();
    }

    @Override
    public void saveSession(SessionInfo sessionInfo) {
        UserSession userSession = new UserSession(
                sessionInfo.userId(),
                sessionInfo.email(),
                sessionInfo.fullName(),
                sessionInfo.username(),
                sessionInfo.avatarUrl(),
                true);
        prefsManager.saveUserSession(userSession);
    }

    @Override
    public SessionInfo getSession() {
        UserSession userSession = prefsManager.getUserSession();
        if (userSession == null) {
            return null;
        }
        return new SessionInfo(
                userSession.userId(),
                userSession.email(),
                userSession.fullName(),
                userSession.username(),
                userSession.avatarUrl());
    }

    @Override
    public void clearSession() {
        prefsManager.clearSession();
    }

    @Override
    public void setRememberMe(boolean rememberMe) {
        prefsManager.setRememberMe(rememberMe);
    }

    @Override
    public boolean isRememberMe() {
        return prefsManager.isRememberMe();
    }

    @Override
    public void setTheme(Theme theme) {
        ThemeMode mode =
                switch (theme) {
                    case DARK -> ThemeMode.DARK;
                    case LIGHT -> ThemeMode.LIGHT;
                    case SYSTEM -> ThemeMode.SYSTEM;
                };
        prefsManager.setThemeMode(mode);
    }

    @Override
    public Theme getTheme() {
        ThemeMode mode = prefsManager.getThemeMode();
        return switch (mode) {
            case DARK -> Theme.DARK;
            case LIGHT -> Theme.LIGHT;
            case SYSTEM -> Theme.SYSTEM;
        };
    }

    @Override
    public void setLanguage(String language) {
        prefsManager.setLanguage(language);
    }

    @Override
    public String getLanguage() {
        return prefsManager.getLanguage();
    }

    @Override
    public void setLastMicEnabled(boolean enabled) {
        prefsManager.setLastMicEnabled(enabled);
    }

    @Override
    public boolean getLastMicEnabled() {
        return prefsManager.getLastMicEnabled();
    }

    @Override
    public void setLastCameraEnabled(boolean enabled) {
        prefsManager.setLastCameraEnabled(enabled);
    }

    @Override
    public boolean getLastCameraEnabled() {
        return prefsManager.getLastCameraEnabled();
    }

    @Override
    public void updateUserProfile(String fullName, String username, String avatarUrl) {
        UserSession currentSession = prefsManager.getUserSession();
        if (currentSession != null) {
            UserSession updatedSession = new UserSession(
                    currentSession.userId(),
                    currentSession.email(),
                    fullName != null ? fullName : currentSession.fullName(),
                    username != null ? username : currentSession.username(),
                    avatarUrl,
                    currentSession.rememberMe());
            prefsManager.saveUserSession(updatedSession);
        }
    }
}
