package com.example.zeromeeting.core.model.user;

import com.google.gson.annotations.SerializedName;

public class UserResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("email")
    private String email;
    @SerializedName("fullName")
    private String fullName;
    @SerializedName("username")
    private String username;
    @SerializedName("avatarUrl")
    private String avatarUrl;
    @SerializedName("authProvider")
    private String authProvider;
    @SerializedName("preferences")
    private UserPreferencesResponse preferences;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("updatedAt")
    private String updatedAt;

    // TODO: Alt + Insert tạo Getters/Setters
}
