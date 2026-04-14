package com.example.zeromeeting.core.model.meeting;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ScheduleMeetingRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("startTime")
    private String startTime;

    @SerializedName("endTime")
    private String endTime;

    @SerializedName("settings")
    private MeetingSettings settings;

    @SerializedName("invitees")
    private List<String> invitees;

    // --- CÁC HÀM GETTER VÀ SETTER ---

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public MeetingSettings getSettings() { return settings; }
    public void setSettings(MeetingSettings settings) { this.settings = settings; }

    public List<String> getInvitees() { return invitees; }
    public void setInvitees(List<String> invitees) { this.invitees = invitees; }
}
