package io.github.phunguy65.zms.domain.model;

/**
 * Domain model representing settings for creating an instant meeting.
 * Used to encapsulate user preferences before API call.
 */
public class InstantMeetingSettings {

    private final boolean waitingRoomEnabled;

    public InstantMeetingSettings(boolean waitingRoomEnabled) {
        this.waitingRoomEnabled = waitingRoomEnabled;
    }

    /**
     * Creates default settings for instant meeting.
     * Waiting room enabled by default.
     */
    public static InstantMeetingSettings withDefaults() {
        return new InstantMeetingSettings(true);
    }

    public boolean isWaitingRoomEnabled() {
        return waitingRoomEnabled;
    }
}
