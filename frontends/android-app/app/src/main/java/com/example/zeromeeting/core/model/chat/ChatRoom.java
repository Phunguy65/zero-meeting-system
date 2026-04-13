package com.example.zeromeeting.core.model.chat;
import com.google.gson.annotations.SerializedName;

public class ChatRoom {
    @SerializedName("roomId")
    private String roomId;

    @SerializedName("meetingId")
    private String meetingId;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    // TODO: Alt + Insert tạo Getters/Setters
}
