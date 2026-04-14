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

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
