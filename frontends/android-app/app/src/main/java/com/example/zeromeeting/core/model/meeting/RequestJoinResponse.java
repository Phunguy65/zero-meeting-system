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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
