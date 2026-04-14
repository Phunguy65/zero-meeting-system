package com.example.zeromeeting.core.network;

import com.example.zeromeeting.core.utils.TokenManager;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    @Inject
    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        // Bỏ qua việc gắn token nếu đang gọi API đăng nhập hoặc đăng ký
        if (originalRequest.url().encodedPath().contains("/auth/")) {
            return chain.proceed(originalRequest);
        }

        String accessToken = tokenManager.getAccessToken();

        // Nếu có Token, gắn vào Header
        if (accessToken != null) {
            Request newRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + accessToken)
                .build();
            return chain.proceed(newRequest);
        }

        return chain.proceed(originalRequest);
    }
}
