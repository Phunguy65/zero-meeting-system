package com.example.zeromeeting.core.model.auth;

import com.google.gson.annotations.SerializedName;

public class LogoutRequest {

    @SerializedName("refreshToken")
    private String refreshToken;

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
