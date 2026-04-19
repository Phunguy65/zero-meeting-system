package io.github.phunguy65.zms.presentation.main.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.usecase.meeting.CreateMeetingUseCase;
import io.github.phunguy65.zms.presentation.common.util.SingleLiveEvent;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for the Dashboard screen.
 * Handles instant meeting creation from the FAB menu.
 */
@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final CreateMeetingUseCase createMeetingUseCase;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final SingleLiveEvent<MeetingCreationResult> _instantMeetingSuccess =
            new SingleLiveEvent<>();
    public LiveData<MeetingCreationResult> instantMeetingSuccess = _instantMeetingSuccess;

    private final SingleLiveEvent<String> _instantMeetingError = new SingleLiveEvent<>();
    public LiveData<String> instantMeetingError = _instantMeetingError;

    @Inject
    public DashboardViewModel(
            CreateMeetingUseCase createMeetingUseCase, @MainExecutor Executor mainExecutor) {
        this.createMeetingUseCase = createMeetingUseCase;
        this.mainExecutor = mainExecutor;
    }

    /**
     * Creates an instant meeting with default settings.
     * Waiting room is enabled by default.
     */
    public void createInstantMeeting() {
        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

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
                                _instantMeetingError.setValue(errorMessage);
                            } else {
                                _instantMeetingSuccess.setValue(result);
                            }
                        },
                        mainExecutor);
    }
}
