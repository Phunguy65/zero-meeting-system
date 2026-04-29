package io.github.phunguy65.zms.domain.model;

/**
 * Domain model representing a pending join request visible to the host.
 */
public class JoinRequestItem {

    private final String id;
    private final String meetingId;
    private final String displayName;
    private final String requestedAt;

    public JoinRequestItem(String id, String meetingId, String displayName, String requestedAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.displayName = displayName;
        this.requestedAt = requestedAt;
    }

    public String getId() {
        return id;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRequestedAt() {
        return requestedAt;
    }
}
