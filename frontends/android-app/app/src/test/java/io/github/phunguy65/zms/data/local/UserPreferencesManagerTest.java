package io.github.phunguy65.zms.data.local;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.SharedPreferences;
import io.github.phunguy65.zms.data.local.model.AppSettings;
import io.github.phunguy65.zms.data.local.model.ThemeMode;
import io.github.phunguy65.zms.data.local.model.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link UserPreferencesManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class UserPreferencesManagerTest {

    @Mock
    private SharedPreferences prefs;

    @Mock
    private SharedPreferences.Editor editor;

    private UserPreferencesManager manager;

    @Before
    public void setup() {
        when(prefs.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);
        when(editor.remove(anyString())).thenReturn(editor);
        manager = new UserPreferencesManager(prefs);
    }

    // ═══ User Session Tests ═══

    @Test
    public void saveUserSession_persistsAllFields() {
        UserSession session = new UserSession(
                "user123", "test@example.com", "Test User", "testuser", "https://avatar.url", true);

        manager.saveUserSession(session);

        verify(editor).putString("user_id", "user123");
        verify(editor).putString("email", "test@example.com");
        verify(editor).putString("full_name", "Test User");
        verify(editor).putString("username", "testuser");
        verify(editor).putString("avatar_url", "https://avatar.url");
        verify(editor).putBoolean("remember_me", true);
        verify(editor).apply();
    }

    @Test
    public void saveUserSession_withNullAvatarUrl_removesKey() {
        UserSession session =
                new UserSession("user123", "test@example.com", "Test User", "testuser", null, true);

        manager.saveUserSession(session);

        verify(editor).remove("avatar_url");
        verify(editor).apply();
    }

    @Test
    public void getUserSession_whenNoUserId_returnsNull() {
        when(prefs.getString("user_id", null)).thenReturn(null);

        UserSession result = manager.getUserSession();

        assertNull(result);
    }

    @Test
    public void getUserSession_returnsCompleteSession() {
        when(prefs.getString("user_id", null)).thenReturn("user123");
        when(prefs.getString("email", null)).thenReturn("test@example.com");
        when(prefs.getString("full_name", null)).thenReturn("Test User");
        when(prefs.getString("username", null)).thenReturn("testuser");
        when(prefs.getString("avatar_url", null)).thenReturn("https://avatar.url");
        when(prefs.getBoolean("remember_me", false)).thenReturn(true);

        UserSession result = manager.getUserSession();

        assertNotNull(result);
        assertEquals("user123", result.userId());
        assertEquals("test@example.com", result.email());
        assertEquals("Test User", result.fullName());
        assertEquals("testuser", result.username());
        assertEquals("https://avatar.url", result.avatarUrl());
        assertTrue(result.rememberMe());
    }

    @Test
    public void clearSession_removesAllKeys() {
        manager.clearSession();

        verify(editor).remove("user_id");
        verify(editor).remove("email");
        verify(editor).remove("full_name");
        verify(editor).remove("username");
        verify(editor).remove("avatar_url");
        verify(editor).remove("remember_me");
        verify(editor).apply();
    }

    @Test
    public void setRememberMe_true_persistsValue() {
        manager.setRememberMe(true);

        verify(editor).putBoolean("remember_me", true);
        verify(editor).apply();
    }

    @Test
    public void setRememberMe_false_persistsValue() {
        manager.setRememberMe(false);

        verify(editor).putBoolean("remember_me", false);
        verify(editor).apply();
    }

    @Test
    public void isRememberMe_returnsPrefValue() {
        when(prefs.getBoolean("remember_me", false)).thenReturn(true);

        assertTrue(manager.isRememberMe());
    }

    @Test
    public void isRememberMe_defaultsFalse() {
        when(prefs.getBoolean("remember_me", false)).thenReturn(false);

        assertFalse(manager.isRememberMe());
    }

    // ═══ Theme Tests ═══

    @Test
    public void setThemeMode_persistsEnumName() {
        manager.setThemeMode(ThemeMode.DARK);

        verify(editor).putString("theme", "DARK");
        verify(editor).apply();
    }

    @Test
    public void getThemeMode_withValidValue_returnsTheme() {
        when(prefs.getString("theme", null)).thenReturn("LIGHT");

        assertEquals(ThemeMode.LIGHT, manager.getThemeMode());
    }

    @Test
    public void getThemeMode_withNullValue_defaultsToSystem() {
        when(prefs.getString("theme", null)).thenReturn(null);

        assertEquals(ThemeMode.SYSTEM, manager.getThemeMode());
    }

    @Test
    public void getThemeMode_withInvalidValue_defaultsToSystem() {
        when(prefs.getString("theme", null)).thenReturn("INVALID");

        assertEquals(ThemeMode.SYSTEM, manager.getThemeMode());
    }

    // ═══ Mic/Camera Tests ═══

    @Test
    public void setLastMicEnabled_persistsValue() {
        manager.setLastMicEnabled(false);

        verify(editor).putBoolean("last_mic_enabled", false);
        verify(editor).apply();
    }

    @Test
    public void getLastMicEnabled_returnsPrefValue() {
        when(prefs.getBoolean("last_mic_enabled", true)).thenReturn(false);

        assertFalse(manager.getLastMicEnabled());
    }

    @Test
    public void getLastMicEnabled_defaultsTrue() {
        when(prefs.getBoolean("last_mic_enabled", true)).thenReturn(true);

        assertTrue(manager.getLastMicEnabled());
    }

    @Test
    public void setLastCameraEnabled_persistsValue() {
        manager.setLastCameraEnabled(false);

        verify(editor).putBoolean("last_camera_enabled", false);
        verify(editor).apply();
    }

    @Test
    public void getLastCameraEnabled_returnsPrefValue() {
        when(prefs.getBoolean("last_camera_enabled", true)).thenReturn(false);

        assertFalse(manager.getLastCameraEnabled());
    }

    @Test
    public void getLastCameraEnabled_defaultsTrue() {
        when(prefs.getBoolean("last_camera_enabled", true)).thenReturn(true);

        assertTrue(manager.getLastCameraEnabled());
    }

    // ═══ Language Tests ═══

    @Test
    public void setLanguage_persistsValue() {
        manager.setLanguage("vi");

        verify(editor).putString("language", "vi");
        verify(editor).apply();
    }

    @Test
    public void getLanguage_returnsPrefValue() {
        when(prefs.getString("language", AppSettings.DEFAULT_LANGUAGE)).thenReturn("vi");

        assertEquals("vi", manager.getLanguage());
    }

    @Test
    public void getLanguage_defaultsToEnglish() {
        when(prefs.getString("language", AppSettings.DEFAULT_LANGUAGE)).thenReturn("en");

        assertEquals("en", manager.getLanguage());
    }
}
