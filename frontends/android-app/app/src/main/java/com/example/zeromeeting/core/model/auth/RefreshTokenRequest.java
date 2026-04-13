package com.example.zeromeeting.core.model.auth;
import com.google.gson.annotations.SerializedName;

public class RefreshTokenRequest {
    @SerializedName("refreshToken")
    private String refreshToken;

    public RefreshTokenRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
