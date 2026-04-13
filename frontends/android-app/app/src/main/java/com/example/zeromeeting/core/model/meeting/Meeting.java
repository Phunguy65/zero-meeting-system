package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class Meeting {
    @SerializedName("id")
    private String id;
    @SerializedName("hostId")
    private String hostId;
    @SerializedName("shortCode")
    private String shortCode;
    @SerializedName("title")
    private String title;
    @SerializedName("description")
    private String description;
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("endTime")
    private String endTime;
    @SerializedName("type")
    private String type; // INSTANT hoặc SCHEDULED
    @SerializedName("status")
    private String status; // SCHEDULED, LIVE, ENDED, CANCELLED
    @SerializedName("settings")
    private MeetingSettings settings;

    // TODO: Alt + Insert tạo Getters/Setters
}
