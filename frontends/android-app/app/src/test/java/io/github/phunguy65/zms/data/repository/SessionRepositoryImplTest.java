package io.github.phunguy65.zms.data.repository;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.data.local.TokenManager;
import io.github.phunguy65.zms.data.local.UserPreferencesManager;
import io.github.phunguy65.zms.data.local.model.ThemeMode;
import io.github.phunguy65.zms.data.local.model.UserSession;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.Theme;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SessionRepositoryImpl}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionRepositoryImplTest {

    @Mock
    private TokenManager tokenManager;

    @Mock
    private UserPreferencesManager prefsManager;

    private SessionRepositoryImpl repository;

    @Before
    public void setup() {
        repository = new SessionRepositoryImpl(tokenManager, prefsManager);
    }

    // ═══ Token Management Tests ═══

    @Test
    public void saveTokens_delegatesToTokenManager() {
        repository.saveTokens("access", "refresh");

        verify(tokenManager).saveTokens("access", "refresh");
    }

    @Test
    public void getAccessToken_delegatesToTokenManager() {
        when(tokenManager.getAccessToken()).thenReturn("token123");

        assertEquals("token123", repository.getAccessToken());
    }

    @Test
    public void getRefreshToken_delegatesToTokenManager() {
        when(tokenManager.getRefreshToken()).thenReturn("refresh456");

        assertEquals("refresh456", repository.getRefreshToken());
    }

    @Test
    public void clearTokens_delegatesToTokenManager() {
        repository.clearTokens();

        verify(tokenManager).clearTokens();
    }

    @Test
    public void hasTokens_delegatesToTokenManager() {
        when(tokenManager.hasTokens()).thenReturn(true);

        assertTrue(repository.hasTokens());
    }

    // ═══ Session Management Tests ═══

    @Test
    public void saveSession_convertsToUserSessionAndSaves() {
        SessionInfo sessionInfo = new SessionInfo(
                "user123", "test@example.com", "Test User", "testuser", "https://avatar.url");

        repository.saveSession(sessionInfo);

        ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
        verify(prefsManager).saveUserSession(captor.capture());
        UserSession saved = captor.getValue();
        assertEquals("user123", saved.userId());
        assertEquals("test@example.com", saved.email());
        assertEquals("Test User", saved.fullName());
        assertEquals("testuser", saved.username());
        assertEquals("https://avatar.url", saved.avatarUrl());
        assertTrue(saved.rememberMe());
    }

    @Test
    public void getSession_whenNoSession_returnsNull() {
        when(prefsManager.getUserSession()).thenReturn(null);

        assertNull(repository.getSession());
    }

    @Test
    public void getSession_convertsFromUserSession() {
        UserSession userSession = new UserSession(
                "user123", "test@example.com", "Test User", "testuser", "https://avatar.url", true);
        when(prefsManager.getUserSession()).thenReturn(userSession);

        SessionInfo result = repository.getSession();

        assertNotNull(result);
        assertEquals("user123", result.userId());
        assertEquals("test@example.com", result.email());
        assertEquals("Test User", result.fullName());
        assertEquals("testuser", result.username());
        assertEquals("https://avatar.url", result.avatarUrl());
    }

    @Test
    public void clearSession_delegatesToPrefsManager() {
        repository.clearSession();

        verify(prefsManager).clearSession();
    }

    @Test
    public void setRememberMe_delegatesToPrefsManager() {
        repository.setRememberMe(true);

        verify(prefsManager).setRememberMe(true);
    }

    @Test
    public void isRememberMe_delegatesToPrefsManager() {
        when(prefsManager.isRememberMe()).thenReturn(true);

        assertTrue(repository.isRememberMe());
    }

    // ═══ Theme Tests ═══

    @Test
    public void setTheme_dark_setsThemeModeDark() {
        repository.setTheme(Theme.DARK);

        verify(prefsManager).setThemeMode(ThemeMode.DARK);
    }

    @Test
    public void setTheme_light_setsThemeModeLight() {
        repository.setTheme(Theme.LIGHT);

        verify(prefsManager).setThemeMode(ThemeMode.LIGHT);
    }

    @Test
    public void setTheme_system_setsThemeModeSystem() {
        repository.setTheme(Theme.SYSTEM);

        verify(prefsManager).setThemeMode(ThemeMode.SYSTEM);
    }

    @Test
    public void getTheme_dark_returnsThemeDark() {
        when(prefsManager.getThemeMode()).thenReturn(ThemeMode.DARK);

        assertEquals(Theme.DARK, repository.getTheme());
    }

    @Test
    public void getTheme_light_returnsThemeLight() {
        when(prefsManager.getThemeMode()).thenReturn(ThemeMode.LIGHT);

        assertEquals(Theme.LIGHT, repository.getTheme());
    }

    @Test
    public void getTheme_system_returnsThemeSystem() {
        when(prefsManager.getThemeMode()).thenReturn(ThemeMode.SYSTEM);

        assertEquals(Theme.SYSTEM, repository.getTheme());
    }

    // ═══ Mic/Camera Tests ═══

    @Test
    public void setLastMicEnabled_delegatesToPrefsManager() {
        repository.setLastMicEnabled(false);

        verify(prefsManager).setLastMicEnabled(false);
    }

    @Test
    public void getLastMicEnabled_delegatesToPrefsManager() {
        when(prefsManager.getLastMicEnabled()).thenReturn(false);

        assertFalse(repository.getLastMicEnabled());
    }

    @Test
    public void setLastCameraEnabled_delegatesToPrefsManager() {
        repository.setLastCameraEnabled(true);

        verify(prefsManager).setLastCameraEnabled(true);
    }

    @Test
    public void getLastCameraEnabled_delegatesToPrefsManager() {
        when(prefsManager.getLastCameraEnabled()).thenReturn(true);

        assertTrue(repository.getLastCameraEnabled());
    }

    // ═══ Compound Operation Tests ═══

    @Test
    public void clearAllSessionData_clearsTokensSessionAndRememberMe() {
        repository.clearAllSessionData();

        verify(tokenManager).clearTokens();
        verify(prefsManager).clearSession();
        verify(prefsManager).setRememberMe(false);
    }

    // ═══ Language Tests ═══

    @Test
    public void setLanguage_delegatesToPrefsManager() {
        repository.setLanguage("vi");

        verify(prefsManager).setLanguage("vi");
    }

    @Test
    public void getLanguage_delegatesToPrefsManager() {
        when(prefsManager.getLanguage()).thenReturn("vi");

        assertEquals("vi", repository.getLanguage());
    }
}
