package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ScheduleMeetingRequest {
    private String title;
    private String description;
    private String startTime;
    private String endTime;
    private MeetingSettings settings;
    private List<String> invitees; // Danh sách email mời

    // TODO: Alt + Insert tạo Getters/Setters
}
