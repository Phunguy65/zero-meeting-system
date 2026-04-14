package com.example.zeromeeting.core.model.auth;

import com.google.gson.annotations.SerializedName;
import com.example.zeromeeting.core.model.user.UserPreferencesResponse;

public class LoginResponse {
    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("expiresIn")
    private long expiresIn;

    @SerializedName("preferences")
    private UserPreferencesResponse preferences;

    // --- CÁC HÀM GETTER VÀ SETTER ---

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserPreferencesResponse getPreferences() {
        return preferences;
    }

    public void setPreferences(UserPreferencesResponse preferences) {
        this.preferences = preferences;
    }
}
