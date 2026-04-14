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

    // --- CÁC HÀM GETTER VÀ SETTER ---

    public String getAdmissionPolicy() { return admissionPolicy; }
    public void setAdmissionPolicy(String admissionPolicy) { this.admissionPolicy = admissionPolicy; }

    public boolean isAllowGuest() { return allowGuest; }
    public void setAllowGuest(boolean allowGuest) { this.allowGuest = allowGuest; }

    public boolean isMuteOnEntry() { return muteOnEntry; }
    public void setMuteOnEntry(boolean muteOnEntry) { this.muteOnEntry = muteOnEntry; }

    public boolean isRecordingEnabled() { return recordingEnabled; }
    public void setRecordingEnabled(boolean recordingEnabled) { this.recordingEnabled = recordingEnabled; }

    public boolean isChatEnabled() { return chatEnabled; }
    public void setChatEnabled(boolean chatEnabled) { this.chatEnabled = chatEnabled; }
}
