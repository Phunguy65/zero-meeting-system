package com.example.zeromeeting.core.di;

import com.example.zeromeeting.core.network.ApiService;
import com.example.zeromeeting.core.network.AuthInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    // LƯU Ý QUAN TRỌNG VỀ BASE_URL:
    // Vì Backend của bạn dùng Microservices (nhiều port 42185, 33825...),
    // giả định là bạn của bạn sẽ dựng 1 cái "API Gateway" chạy chung ở 1 port (ví dụ 8080).
    // Nếu chạy trên máy ảo Android (Emulator), localhost của máy tính sẽ là 10.0.2.2
    private static final String BASE_URL = "http://10.0.2.2:8080/";

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        // Set Level.BODY để in ra toàn bộ request/response trong Logcat (rất tiện để Debug)
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        return interceptor;
    }

    // Sửa lại hàm này
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
        HttpLoggingInterceptor loggingInterceptor,
        AuthInterceptor authInterceptor // <-- Bơm thêm AuthInterceptor vào đây
    ) {
        return new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor) // <-- Gắn vào OkHttp
            .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Tự động convert JSON thành Model
            .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        // Hilt sẽ tự động gọi hàm này và cung cấp ApiService cho toàn bộ app
        return retrofit.create(ApiService.class);
    }
}
