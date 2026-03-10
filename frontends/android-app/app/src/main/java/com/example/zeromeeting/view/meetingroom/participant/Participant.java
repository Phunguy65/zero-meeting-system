package com.example.zeromeeting.view.meetingroom.participant;

public class Participant {
    private String name;
    private String roleStatus; // (Host, Me)
    private String connectionStatus; // Connecting...
    private boolean isMicOn;
    private boolean isVideoOn;
    private boolean hasAlert; // Chấm đỏ

    public Participant(String name, String roleStatus, String connectionStatus, boolean isMicOn, boolean isVideoOn, boolean hasAlert) {
        this.name = name;
        this.roleStatus = roleStatus;
        this.connectionStatus = connectionStatus;
        this.isMicOn = isMicOn;
        this.isVideoOn = isVideoOn;
        this.hasAlert = hasAlert;
    }

    // Bạn dùng Generate (Alt+Insert / Cmd+N) để tạo các hàm Getter ở đây nhé
    public String getName() { return name; }
    public String getRoleStatus() { return roleStatus; }
    public String getConnectionStatus() { return connectionStatus; }
    public boolean isMicOn() { return isMicOn; }
    public boolean isVideoOn() { return isVideoOn; }
    public boolean isHasAlert() { return hasAlert; }
}
