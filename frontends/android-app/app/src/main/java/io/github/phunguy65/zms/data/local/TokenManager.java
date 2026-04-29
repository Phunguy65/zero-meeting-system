package io.github.phunguy65.zms.data.local;

import android.content.SharedPreferences;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages authentication token persistence using {@link android.content.SharedPreferences}
 * backed by {@link androidx.security.crypto.EncryptedSharedPreferences}.
 *
 * <p>Stores {@code accessToken} and {@code refreshToken} encrypted at rest
 * (AES256-GCM for keys, AES256-SIV for values). Provided as a Hilt singleton
 * via {@link io.github.phunguy65.zms.di.StorageModule}.
 */
@Singleton
public class TokenManager {

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private final SharedPreferences prefs;

    @Inject
    public TokenManager(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    /** Persists both tokens atomically. */
    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    /** Returns the stored access token, or {@code null} if none exists. */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /** Returns the stored refresh token, or {@code null} if none exists. */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /** Removes both tokens from storage. */
    public void clearTokens() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply();
    }

    /** Returns {@code true} if an access token is stored. */
    public boolean hasTokens() {
        return prefs.contains(KEY_ACCESS_TOKEN);
    }
}
