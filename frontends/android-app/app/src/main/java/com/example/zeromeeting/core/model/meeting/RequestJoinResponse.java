package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class RequestJoinResponse {
    @SerializedName("requestId")
    private String requestId;

    @SerializedName("status")
    private String status; // PENDING, APPROVED, DENIED, EXPIRED

    @SerializedName("token")
    private String token;

    @SerializedName("roomName")
    private String roomName;

    // TODO: Alt + Insert tạo Getters/Setters
}
