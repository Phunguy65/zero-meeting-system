package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class JoinMeetingRequest {
    @SerializedName("displayName")
    private String displayName;

    @SerializedName("deviceId")
    private String deviceId;

    @SerializedName("password")
    private String password;

    public JoinMeetingRequest(String displayName, String deviceId, String password) {
        this.displayName = displayName;
        this.deviceId = deviceId;
        this.password = password;
    }
    // TODO: Alt + Insert tạo Getters/Setters
}
