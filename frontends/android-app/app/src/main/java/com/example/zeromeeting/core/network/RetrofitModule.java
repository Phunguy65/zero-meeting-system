package com.example.zeromeeting.core.network;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class RetrofitModule {

    // URL gốc của Server backend (Sửa lại khi có API thật)
    private static final String BASE_URL = "https://api.zeromeeting.com/";

    @Provides
    @Singleton
    public Retrofit provideRetrofit() {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Dùng Gson để parse JSON
            // .client(okHttpClient) // Sau này thêm OkHttp để gắn Token vào Header
            .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
