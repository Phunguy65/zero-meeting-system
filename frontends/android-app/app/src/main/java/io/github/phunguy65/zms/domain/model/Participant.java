package io.github.phunguy65.zms.domain.model;

/**
 * Domain model representing a meeting participant with real-time media state
 * and backend role metadata merged together.
 */
public class Participant {

    private final String id;
    private final String name;
    private final ParticipantRole role;
    private final boolean isMicOn;
    private final boolean isVideoOn;
    private final boolean isLocal;

    public Participant(
            String id,
            String name,
            ParticipantRole role,
            boolean isMicOn,
            boolean isVideoOn,
            boolean isLocal) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.isMicOn = isMicOn;
        this.isVideoOn = isVideoOn;
        this.isLocal = isLocal;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public boolean isMicOn() {
        return isMicOn;
    }

    public boolean isVideoOn() {
        return isVideoOn;
    }

    public boolean isLocal() {
        return isLocal;
    }
}
