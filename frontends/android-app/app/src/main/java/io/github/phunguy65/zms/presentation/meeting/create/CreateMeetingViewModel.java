package io.github.phunguy65.zms.presentation.meeting.create;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.meeting.CreateMeetingUseCase;
import io.github.phunguy65.zms.presentation.common.util.SingleLiveEvent;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for creating new meetings.
 *
 * <p>Handles meeting creation and persists mic/camera states to preferences.
 */
@HiltViewModel
public class CreateMeetingViewModel extends ViewModel {

    private final SessionRepository sessionRepository;
    private final CreateMeetingUseCase createMeetingUseCase;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<MeetingCreationResult> _meetingResult = new MutableLiveData<>();
    public LiveData<MeetingCreationResult> meetingResult = _meetingResult;

    private final SingleLiveEvent<MeetingCreationResult> _meetingSuccess = new SingleLiveEvent<>();
    public LiveData<MeetingCreationResult> meetingSuccess = _meetingSuccess;

    private final SingleLiveEvent<String> _meetingError = new SingleLiveEvent<>();
    public LiveData<String> meetingError = _meetingError;

    @Inject
    public CreateMeetingViewModel(
            SessionRepository sessionRepository,
            CreateMeetingUseCase createMeetingUseCase,
            @MainExecutor Executor mainExecutor) {
        this.sessionRepository = sessionRepository;
        this.createMeetingUseCase = createMeetingUseCase;
        this.mainExecutor = mainExecutor;
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
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        sessionRepository.setLastMicEnabled(isAudioOn);
        sessionRepository.setLastCameraEnabled(isVideoOn);

        _isLoading.setValue(true);

        InstantMeetingSettings settings = InstantMeetingSettings.withDefaults();

        createMeetingUseCase
                .execute(settings)
                .whenCompleteAsync(
                        (result, error) -> {
                            _isLoading.setValue(false);

                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _meetingError.setValue(errorMessage);
                            } else {
                                _meetingResult.setValue(result);
                                _meetingSuccess.setValue(result);
                            }
                        },
                        mainExecutor);
    }

    /**
     * Returns the meeting link if a meeting has been created.
     * @return the meeting short code or null if not yet created
     */
    public String getMeetingLink() {
        MeetingCreationResult result = _meetingResult.getValue();
        return result != null ? result.getShortCode() : null;
    }
}
