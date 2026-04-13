package io.github.phunguy65.zms.di;

import android.content.Context;
import androidx.core.content.ContextCompat;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.inject.Singleton;

/** Hilt module providing thread executors for async operations. */
@Module
@InstallIn(SingletonComponent.class)
public class ExecutorModule {

    private static final int IO_THREAD_POOL_SIZE = 4;

    /**
     * Provides an I/O-bound executor for network, database, and file operations.
     *
     * <p>Uses a fixed thread pool to limit concurrent I/O operations and prevent resource
     * exhaustion.
     */
    @Provides
    @Singleton
    @IoExecutor
    public Executor provideIoExecutor() {
        return Executors.newFixedThreadPool(IO_THREAD_POOL_SIZE);
    }

    /**
     * Provides the main thread executor for UI updates.
     *
     * <p>Uses Android's main executor via ContextCompat for lifecycle-safe main thread posting.
     */
    @Provides
    @Singleton
    @MainExecutor
    public Executor provideMainExecutor(@ApplicationContext Context context) {
        return ContextCompat.getMainExecutor(context);
    }
}
