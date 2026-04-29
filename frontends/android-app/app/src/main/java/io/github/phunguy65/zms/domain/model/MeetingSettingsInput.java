package io.github.phunguy65.zms.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Domain value object encapsulating meeting settings selected by the user.
 *
 * <p>Used by scheduled meeting requests to carry all backend-supported settings
 * using the simplified meeting settings contract: admission policy (waiting room),
 * allowGuest, maxParticipants, allowScreenShare, chatEnabled, allowMicrophone,
 * allowVideo, and optional password.
 */
public class MeetingSettingsInput {

    /** Waiting room enabled maps to MANUAL_APPROVAL admission policy. */
    private final boolean waitingRoomEnabled;

    /** Whether guests are allowed to join without authentication. */
    private final boolean allowGuest;

    /** Optional meeting password. Null or empty means no password. */
    @Nullable private final String password;

    /** Maximum number of participants allowed. */
    private final int maxParticipants;

    /** Whether participants can share their screen. */
    private final boolean allowScreenShare;

    /** Whether chat is enabled during the meeting. */
    private final boolean chatEnabled;

    /** Whether participants can use their microphone. */
    private final boolean allowMicrophone;

    /** Whether participants can use their video. */
    private final boolean allowVideo;

    public MeetingSettingsInput(
            boolean waitingRoomEnabled,
            boolean allowGuest,
            @Nullable String password,
            int maxParticipants,
            boolean allowScreenShare,
            boolean chatEnabled,
            boolean allowMicrophone,
            boolean allowVideo) {
        this.waitingRoomEnabled = waitingRoomEnabled;
        this.allowGuest = allowGuest;
        this.password = password;
        this.maxParticipants = maxParticipants;
        this.allowScreenShare = allowScreenShare;
        this.chatEnabled = chatEnabled;
        this.allowMicrophone = allowMicrophone;
        this.allowVideo = allowVideo;
    }

    /**
     * Creates default settings matching the backend's simplified defaults.
     * Defaults: allowGuest=true, allowScreenShare=true, allowMicrophone=true,
     * allowVideo=true, chatEnabled=true, maxParticipants=100.
     */
    public static MeetingSettingsInput defaults() {
        return new MeetingSettingsInput(
                true, // waitingRoomEnabled
                true, // allowGuest
                null, // password
                100, // maxParticipants
                true, // allowScreenShare
                true, // chatEnabled
                true, // allowMicrophone
                true // allowVideo
                );
    }

    public boolean isWaitingRoomEnabled() {
        return waitingRoomEnabled;
    }

    public boolean isAllowGuest() {
        return allowGuest;
    }

    @Nullable public String getPassword() {
        return password;
    }

    /**
     * Returns true if a non-empty password is set.
     */
    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public boolean isAllowScreenShare() {
        return allowScreenShare;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public boolean isAllowMicrophone() {
        return allowMicrophone;
    }

    public boolean isAllowVideo() {
        return allowVideo;
    }
}
