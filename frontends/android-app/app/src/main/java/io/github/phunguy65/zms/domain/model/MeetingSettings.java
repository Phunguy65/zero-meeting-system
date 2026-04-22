package io.github.phunguy65.zms.domain.model;

import org.jspecify.annotations.Nullable;

/**
 * Domain model representing meeting settings state for display and editing.
 *
 * <p>Used by in-meeting settings bottom sheet and pre-meeting edit mode to show
 * current settings and submit updates. Immutable value object with builder pattern
 * for creating modified copies.
 */
public final class MeetingSettings {

    private final boolean waitingRoomEnabled;
    private final boolean allowGuest;

    @Nullable private final String password;

    private final boolean requirePassword;
    private final int maxParticipants;
    private final boolean allowScreenShare;
    private final boolean chatEnabled;
    private final boolean allowMicrophone;
    private final boolean allowVideo;

    private MeetingSettings(Builder builder) {
        this.waitingRoomEnabled = builder.waitingRoomEnabled;
        this.allowGuest = builder.allowGuest;
        this.password = builder.password;
        this.requirePassword = builder.requirePassword;
        this.maxParticipants = builder.maxParticipants;
        this.allowScreenShare = builder.allowScreenShare;
        this.chatEnabled = builder.chatEnabled;
        this.allowMicrophone = builder.allowMicrophone;
        this.allowVideo = builder.allowVideo;
    }

    public static MeetingSettings defaults() {
        return new Builder()
                .waitingRoomEnabled(true)
                .allowGuest(true)
                .maxParticipants(100)
                .allowScreenShare(true)
                .chatEnabled(true)
                .allowMicrophone(true)
                .allowVideo(true)
                .build();
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

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }

    public boolean isRequirePassword() {
        return requirePassword;
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

    public Builder toBuilder() {
        return new Builder()
                .waitingRoomEnabled(waitingRoomEnabled)
                .allowGuest(allowGuest)
                .password(password)
                .requirePassword(requirePassword)
                .maxParticipants(maxParticipants)
                .allowScreenShare(allowScreenShare)
                .chatEnabled(chatEnabled)
                .allowMicrophone(allowMicrophone)
                .allowVideo(allowVideo);
    }

    public static final class Builder {
        private boolean waitingRoomEnabled = true;
        private boolean allowGuest = true;

        @Nullable private String password = null;

        private boolean requirePassword = false;
        private int maxParticipants = 100;
        private boolean allowScreenShare = true;
        private boolean chatEnabled = true;
        private boolean allowMicrophone = true;
        private boolean allowVideo = true;

        public Builder waitingRoomEnabled(boolean value) {
            this.waitingRoomEnabled = value;
            return this;
        }

        public Builder allowGuest(boolean value) {
            this.allowGuest = value;
            return this;
        }

        public Builder password(@Nullable String value) {
            this.password = value;
            return this;
        }

        public Builder requirePassword(boolean value) {
            this.requirePassword = value;
            return this;
        }

        public Builder maxParticipants(int value) {
            this.maxParticipants = value;
            return this;
        }

        public Builder allowScreenShare(boolean value) {
            this.allowScreenShare = value;
            return this;
        }

        public Builder chatEnabled(boolean value) {
            this.chatEnabled = value;
            return this;
        }

        public Builder allowMicrophone(boolean value) {
            this.allowMicrophone = value;
            return this;
        }

        public Builder allowVideo(boolean value) {
            this.allowVideo = value;
            return this;
        }

        public MeetingSettings build() {
            return new MeetingSettings(this);
        }
    }
}
