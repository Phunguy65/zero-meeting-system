package com.example.zeromeeting.core.network;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import jakarta.inject.Singleton;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class RetrofitModule {

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {

        return new Retrofit.Builder()
                .baseUrl("https://example.com/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

}
