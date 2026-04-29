package io.github.phunguy65.zms.domain.model;

/**
 * Lightweight DTO for backend-provided participant role metadata
 * used to enrich LiveKit participants in the participants sheet.
 */
public class ParticipantRoleInfo {

    private final String id;
    private final String displayName;
    private final ParticipantRole role;

    public ParticipantRoleInfo(String id, String displayName, ParticipantRole role) {
        this.id = id;
        this.displayName = displayName;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ParticipantRole getRole() {
        return role;
    }
}
