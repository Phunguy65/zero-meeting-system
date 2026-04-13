package com.example.zeromeeting.core.model.meeting;
import com.google.gson.annotations.SerializedName;

public class MeetingSettings {
    @SerializedName("admissionPolicy")
    private String admissionPolicy;
    @SerializedName("allowGuest")
    private boolean allowGuest;
    @SerializedName("muteOnEntry")
    private boolean muteOnEntry;
    @SerializedName("recordingEnabled")
    private boolean recordingEnabled;
    @SerializedName("chatEnabled")
    private boolean chatEnabled;

    // TODO: Alt + Insert tạo Getters/Setters
}
