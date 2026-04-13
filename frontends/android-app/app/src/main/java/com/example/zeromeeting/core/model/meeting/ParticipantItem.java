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
}
