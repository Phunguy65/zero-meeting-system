package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.RoomConnectionState;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.livekit.android.room.track.LocalVideoTrack;
import java.util.List;

/**
 * Repository interface for LiveKit room operations.
 * Handles room connection, disconnection, and local media controls.
 */
public interface LiveKitRepository {

    /**
     * Connects to a LiveKit room with the given URL and token.
     *
     * @param url   the LiveKit server WebSocket URL
     * @param token the access token for room authentication
     */
    void connect(String url, String token);

    /**
     * Disconnects from the current LiveKit room.
     * Cleans up all resources and releases tracks.
     */
    void disconnect();

    /**
     * Sets whether the local microphone is enabled.
     *
     * @param enabled true to enable microphone, false to mute
     */
    void setMicrophoneEnabled(boolean enabled);

    /**
     * Sets whether the local camera is enabled.
     *
     * @param enabled true to enable camera, false to disable
     */
    void setCameraEnabled(boolean enabled);

    /**
     * Switches between front and back camera.
     */
    void switchCamera();

    /**
     * Returns the current local video track, or null if camera is disabled.
     *
     * @return the local video track
     */
    LocalVideoTrack getLocalVideoTrack();

    /**
     * Returns whether the local microphone is currently enabled.
     *
     * @return true if microphone is enabled
     */
    boolean isMicrophoneEnabled();

    /**
     * Returns whether the local camera is currently enabled.
     *
     * @return true if camera is enabled
     */
    boolean isCameraEnabled();

    /**
     * Registers a listener for room events.
     *
     * @param listener the listener to register
     */
    void setRoomEventListener(RoomEventListener listener);

    /**
     * Removes the current room event listener.
     */
    void removeRoomEventListener();

    /**
     * Listener interface for LiveKit room events.
     */
    interface RoomEventListener {

        /** Called when the connection state changes. */
        void onConnectionStateChanged(RoomConnectionState state);

        /** Called when a participant joins the room. */
        void onParticipantConnected(VideoParticipant participant);

        /** Called when a participant leaves the room. */
        void onParticipantDisconnected(String participantId);

        /** Called when the participant list is updated (track subscribed/unsubscribed). */
        void onParticipantsUpdated(List<VideoParticipant> participants);

        /** Called when active speakers change. */
        void onActiveSpeakersChanged(List<String> speakerIds);

        /** Called when the local video track becomes available. */
        void onLocalVideoTrackAvailable(LocalVideoTrack track);

        /** Called when a reliable data message is received from a remote participant. */
        default void onDataReceived(byte[] data) {}
    }
}
