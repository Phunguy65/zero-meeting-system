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

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
