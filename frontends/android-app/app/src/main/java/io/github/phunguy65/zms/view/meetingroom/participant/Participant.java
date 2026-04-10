package io.github.phunguy65.zms.view.meetingroom.participant;

public class Participant {
    private final String name;
    private final String roleStatus; // (Host, Me)
    private final String connectionStatus; // Connecting...
    private final boolean isMicOn;
    private final boolean isVideoOn;
    private final boolean hasAlert; // Chấm đỏ

    public Participant(
            String name,
            String roleStatus,
            String connectionStatus,
            boolean isMicOn,
            boolean isVideoOn,
            boolean hasAlert) {
        this.name = name;
        this.roleStatus = roleStatus;
        this.connectionStatus = connectionStatus;
        this.isMicOn = isMicOn;
        this.isVideoOn = isVideoOn;
        this.hasAlert = hasAlert;
    }

    // Bạn dùng Generate (Alt+Insert / Cmd+N) để tạo các hàm Getter ở đây nhé
    public String getName() {
        return name;
    }

    public String getRoleStatus() {
        return roleStatus;
    }

    public String getConnectionStatus() {
        return connectionStatus;
    }

    public boolean isMicOn() {
        return isMicOn;
    }

    public boolean isVideoOn() {
        return isVideoOn;
    }

    public boolean isHasAlert() {
        return hasAlert;
    }
}
