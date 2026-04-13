package com.example.zeromeeting.core.model.auth;
import com.google.gson.annotations.SerializedName;

public class RegisterResponse {
    @SerializedName("userId")
    private String userId;

    @SerializedName("email")
    private String email;

    @SerializedName("fullName")
    private String fullName;

    @SerializedName("username")
    private String username;

    // TODO: Bấm Alt + Insert tạo Getters/Setters
}
