package io.github.phunguy65.zms.domain.model;

public class Participant {
    private final String name;
    private final String roleStatus;
    private final String connectionStatus;
    private final boolean isMicOn;
    private final boolean isVideoOn;
    private final boolean hasAlert;

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
