package io.github.phunguy65.zms.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/** Hilt module providing network dependencies: OkHttpClient, Retrofit, and API interfaces. */
@Module
@InstallIn(SingletonComponent.class)
public abstract class NetworkModule {}
