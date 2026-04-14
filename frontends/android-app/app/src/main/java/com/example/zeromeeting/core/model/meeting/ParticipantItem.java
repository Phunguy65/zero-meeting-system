package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class ParticipantItem {
    @SerializedName("id")
    private long id;

    @SerializedName("meetingId")
    private String meetingId;

    @SerializedName("userId")
    private String userId;

    @SerializedName("displayName")
    private String displayName;

    @SerializedName("role")
    private String role; // HOST, PARTICIPANT, GUEST

    @SerializedName("joinedAt")
    private String joinedAt;

    @SerializedName("leftAt")
    private String leftAt;

    // TODO: Alt + Insert tạo Getters/Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(String joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(String leftAt) {
        this.leftAt = leftAt;
    }
}
