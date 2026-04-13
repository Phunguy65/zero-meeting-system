package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class JoinRequestItem {
    @SerializedName("id")
    private String id;

    @SerializedName("meetingId")
    private String meetingId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("status")
    private String status;

    @SerializedName("requestedAt")
    private String requestedAt;

    // TODO: Alt + Insert tạo Getters/Setters
}
