package io.github.phunguy65.zms.di;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Hilt module providing local storage dependencies.
 *
 * <p>Provides two SharedPreferences instances:
 * <ul>
 *   <li><b>Encrypted</b> (default): For sensitive data like auth tokens, backed by
 *       {@link EncryptedSharedPreferences} with AES256-GCM/SIV encryption</li>
 *   <li><b>User Prefs</b> ({@code @Named("userPrefs")}): For non-sensitive user preferences
 *       like theme, mic/camera state, and session info</li>
 * </ul>
 */
@Module
@InstallIn(SingletonComponent.class)
public final class StorageModule {

    private static final String PREFS_FILE_NAME = "zms_secure_prefs";
    private static final String USER_PREFS_FILE_NAME = "zms_user_prefs";

    @Provides
    @Singleton
    SharedPreferences provideEncryptedSharedPreferences(@ApplicationContext Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            return EncryptedSharedPreferences.create(
                    PREFS_FILE_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create EncryptedSharedPreferences", e);
        }
    }

    /**
     * Provides a standard SharedPreferences instance for non-sensitive user preferences.
     *
     * <p>Used by {@link io.github.phunguy65.zms.data.local.UserPreferencesManager} to store
     * theme settings, mic/camera states, and cached user session info.
     */
    @Provides
    @Singleton
    @Named("userPrefs")
    SharedPreferences provideUserPreferences(@ApplicationContext Context context) {
        return context.getSharedPreferences(USER_PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }
}
