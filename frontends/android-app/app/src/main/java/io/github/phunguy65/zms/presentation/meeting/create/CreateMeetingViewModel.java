package io.github.phunguy65.zms.presentation.meeting.create;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;

/**
 * ViewModel for creating new meetings.
 *
 * <p>Handles meeting creation and persists mic/camera states to preferences.
 */
@HiltViewModel
public class CreateMeetingViewModel extends ViewModel {

    private final SessionRepository sessionRepository;

    @Inject
    public CreateMeetingViewModel(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Gets the last saved mic enabled state.
     *
     * @return true if mic was enabled in the last meeting
     */
    public boolean getLastMicEnabled() {
        return sessionRepository.getLastMicEnabled();
    }

    /**
     * Gets the last saved camera enabled state.
     *
     * @return true if camera was enabled in the last meeting
     */
    public boolean getLastCameraEnabled() {
        return sessionRepository.getLastCameraEnabled();
    }

    /**
     * Starts a new meeting with the specified audio/video settings.
     * Saves the settings to preferences before starting.
     *
     * @param isVideoOn whether to start with camera enabled
     * @param isAudioOn whether to start with mic enabled
     */
    public void startNewMeeting(boolean isVideoOn, boolean isAudioOn) {
        sessionRepository.setLastMicEnabled(isAudioOn);
        sessionRepository.setLastCameraEnabled(isVideoOn);

        // TODO: Logic to create meeting via API
    }
}
