package com.example.zeromeeting.core.model.user;

import com.google.gson.annotations.SerializedName;

public class DeleteAccountResponse {
    @SerializedName("userId")
    private String userId;
    @SerializedName("email")
    private String email;
    @SerializedName("fullName")
    private String fullName;
    @SerializedName("deletedAt")
    private String deletedAt;

    // TODO: Alt + Insert tạo Getters/Setters
}
