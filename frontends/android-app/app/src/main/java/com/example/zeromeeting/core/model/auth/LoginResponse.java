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

    // TODO: Bấm Alt + Insert tạo Getters/Setters
}
