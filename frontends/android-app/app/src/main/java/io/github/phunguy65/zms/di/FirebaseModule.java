package io.github.phunguy65.zms.di;

import com.google.firebase.storage.FirebaseStorage;
import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import io.github.phunguy65.zms.data.remote.firebase.AvatarStorageManager;
import io.github.phunguy65.zms.domain.repository.AvatarRepository;
import javax.inject.Singleton;

/**
 * Hilt module providing Firebase dependencies.
 *
 * <p>Provides Firebase Storage for avatar uploads.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class FirebaseModule {

    @Binds
    @Singleton
    abstract AvatarRepository bindAvatarRepository(AvatarStorageManager impl);

    @Provides
    @Singleton
    static FirebaseStorage provideFirebaseStorage() {
        return FirebaseStorage.getInstance();
    }
}
