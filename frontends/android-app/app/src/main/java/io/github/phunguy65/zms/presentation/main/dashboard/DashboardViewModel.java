package io.github.phunguy65.zms.presentation.main.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import io.github.phunguy65.zms.domain.usecase.meeting.CancelMeetingUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.CreateMeetingUseCase;
import io.github.phunguy65.zms.domain.usecase.meeting.GetUpcomingMeetingsUseCase;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;
import io.github.phunguy65.zms.presentation.common.util.SingleLiveEvent;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel for the Dashboard screen.
 * Handles instant meeting creation from the FAB menu and upcoming meetings loading.
 */
@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final CreateMeetingUseCase createMeetingUseCase;
    private final GetUpcomingMeetingsUseCase getUpcomingMeetingsUseCase;
    private final CancelMeetingUseCase cancelMeetingUseCase;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final SingleLiveEvent<MeetingCreationResult> _instantMeetingSuccess =
            new SingleLiveEvent<>();
    public LiveData<MeetingCreationResult> instantMeetingSuccess = _instantMeetingSuccess;

    private final SingleLiveEvent<String> _instantMeetingError = new SingleLiveEvent<>();
    public LiveData<String> instantMeetingError = _instantMeetingError;

    private final MutableLiveData<Boolean> _isCancelling = new MutableLiveData<>(false);
    public LiveData<Boolean> isCancelling = _isCancelling;

    private final SingleLiveEvent<String> _cancelSuccess = new SingleLiveEvent<>();
    public LiveData<String> cancelSuccess = _cancelSuccess;

    private final SingleLiveEvent<String> _cancelError = new SingleLiveEvent<>();
    public LiveData<String> cancelError = _cancelError;

    private final MutableLiveData<UiState<List<UpcomingMeeting>>> _upcomingMeetingsState =
            new MutableLiveData<>(new UiState.Loading<>());
    public LiveData<UiState<List<UpcomingMeeting>>> upcomingMeetingsState = _upcomingMeetingsState;

    @Inject
    public DashboardViewModel(
            CreateMeetingUseCase createMeetingUseCase,
            GetUpcomingMeetingsUseCase getUpcomingMeetingsUseCase,
            CancelMeetingUseCase cancelMeetingUseCase,
            @MainExecutor Executor mainExecutor) {
        this.createMeetingUseCase = createMeetingUseCase;
        this.getUpcomingMeetingsUseCase = getUpcomingMeetingsUseCase;
        this.cancelMeetingUseCase = cancelMeetingUseCase;
        this.mainExecutor = mainExecutor;

        loadUpcomingMeetings();
    }

    /**
     * Loads upcoming host meetings for the dashboard.
     */
    public void loadUpcomingMeetings() {
        _upcomingMeetingsState.setValue(new UiState.Loading<>());

        getUpcomingMeetingsUseCase
                .execute()
                .whenCompleteAsync(
                        (meetings, error) -> {
                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _upcomingMeetingsState.setValue(
                                        new UiState.Error<>(new UiError.Unknown(errorMessage)));
                            } else {
                                _upcomingMeetingsState.setValue(new UiState.Success<>(meetings));
                            }
                        },
                        mainExecutor);
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

    /**
     * Cancels a scheduled meeting.
     * Reloads upcoming meetings on success.
     *
     * @param meetingId the meeting UUID to cancel
     */
    public void cancelMeeting(String meetingId) {
        if (Boolean.TRUE.equals(_isCancelling.getValue())) {
            return;
        }

        _isCancelling.setValue(true);

        cancelMeetingUseCase
                .execute(meetingId)
                .whenCompleteAsync(
                        (result, error) -> {
                            _isCancelling.setValue(false);

                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _cancelError.setValue(errorMessage);
                            } else {
                                _cancelSuccess.setValue(meetingId);
                                loadUpcomingMeetings();
                            }
                        },
                        mainExecutor);
    }
}
