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
import javax.inject.Singleton;

/**
 * Hilt module providing local storage dependencies.
 *
 * <p>Creates an {@link EncryptedSharedPreferences} instance backed by a
 * {@link MasterKeys#AES256_GCM_SPEC} master key. The encrypted prefs are used
 * by {@link io.github.phunguy65.zms.data.local.TokenManager} to store
 * authentication tokens at rest.
 */
@Module
@InstallIn(SingletonComponent.class)
public final class StorageModule {

    private static final String PREFS_FILE_NAME = "zms_secure_prefs";

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
}
