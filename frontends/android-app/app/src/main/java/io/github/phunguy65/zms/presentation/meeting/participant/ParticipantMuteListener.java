package io.github.phunguy65.zms.presentation.meeting.participant;

/**
 * Callback interface for host-initiated mute actions on individual participants.
 * Implemented by the containing UI component and passed to {@link ParticipantAdapter}.
 */
public interface ParticipantMuteListener {

    /**
     * Called when the host taps the microphone button for a remote participant.
     *
     * @param identity the LiveKit participant identity of the target participant
     */
    void onMuteMic(String identity);

    /**
     * Called when the host taps the camera button for a remote participant.
     *
     * @param identity the LiveKit participant identity of the target participant
     */
    void onMuteCamera(String identity);
}
