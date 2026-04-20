package io.github.phunguy65.zms.domain.model;

import io.livekit.android.room.track.VideoTrack;

/**
 * Represents a participant in a video call with their video track state.
 */
public class VideoParticipant {

    private final String id;
    private final String displayName;
    private final VideoTrack videoTrack;
    private final boolean isMicEnabled;
    private final boolean isCameraEnabled;
    private final boolean isActiveSpeaker;
    private final boolean isLocal;

    public VideoParticipant(
            String id,
            String displayName,
            VideoTrack videoTrack,
            boolean isMicEnabled,
            boolean isCameraEnabled,
            boolean isActiveSpeaker,
            boolean isLocal) {
        this.id = id;
        this.displayName = displayName;
        this.videoTrack = videoTrack;
        this.isMicEnabled = isMicEnabled;
        this.isCameraEnabled = isCameraEnabled;
        this.isActiveSpeaker = isActiveSpeaker;
        this.isLocal = isLocal;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VideoTrack getVideoTrack() {
        return videoTrack;
    }

    public boolean isMicEnabled() {
        return isMicEnabled;
    }

    public boolean isCameraEnabled() {
        return isCameraEnabled;
    }

    public boolean isActiveSpeaker() {
        return isActiveSpeaker;
    }

    public boolean isLocal() {
        return isLocal;
    }

    /**
     * Creates a copy of this participant with updated active speaker state.
     */
    public VideoParticipant withActiveSpeaker(boolean isActiveSpeaker) {
        return new VideoParticipant(
                id, displayName, videoTrack, isMicEnabled, isCameraEnabled, isActiveSpeaker, isLocal);
    }

    /**
     * Creates a copy of this participant with updated video track.
     */
    public VideoParticipant withVideoTrack(VideoTrack videoTrack) {
        return new VideoParticipant(
                id, displayName, videoTrack, isMicEnabled, isCameraEnabled, isActiveSpeaker, isLocal);
    }

    /**
     * Creates a copy of this participant with updated mic state.
     */
    public VideoParticipant withMicEnabled(boolean isMicEnabled) {
        return new VideoParticipant(
                id, displayName, videoTrack, isMicEnabled, isCameraEnabled, isActiveSpeaker, isLocal);
    }

    /**
     * Creates a copy of this participant with updated camera state.
     */
    public VideoParticipant withCameraEnabled(boolean isCameraEnabled) {
        return new VideoParticipant(
                id, displayName, videoTrack, isMicEnabled, isCameraEnabled, isActiveSpeaker, isLocal);
    }
}
