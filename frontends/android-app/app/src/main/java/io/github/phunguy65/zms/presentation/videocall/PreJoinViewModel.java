package io.github.phunguy65.zms.presentation.videocall;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import javax.inject.Inject;

/**
 * ViewModel for the pre-join screen.
 *
 * <p>Manages mic/camera state persistence for meeting join.
 */
@HiltViewModel
public class PreJoinViewModel extends ViewModel {

    private final SessionRepository sessionRepository;

    @Inject
    public PreJoinViewModel(SessionRepository sessionRepository) {
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
     * Saves the mic enabled state.
     *
     * @param enabled whether mic is enabled
     */
    public void setLastMicEnabled(boolean enabled) {
        sessionRepository.setLastMicEnabled(enabled);
    }

    /**
     * Saves the camera enabled state.
     *
     * @param enabled whether camera is enabled
     */
    public void setLastCameraEnabled(boolean enabled) {
        sessionRepository.setLastCameraEnabled(enabled);
    }
}
