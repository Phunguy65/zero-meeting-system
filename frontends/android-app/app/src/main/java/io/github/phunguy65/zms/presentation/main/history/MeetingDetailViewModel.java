package io.github.phunguy65.zms.presentation.main.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.history.GetMeetingDetailUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel driving the meeting detail screen.
 *
 * <p>Loads a single meeting history detail (participants + recordings) by the {@code meetingId}
 * argument supplied through {@link SavedStateHandle}.
 */
@HiltViewModel
public class MeetingDetailViewModel extends ViewModel {

    /** Safe Args argument name for meeting id, matches nav_graph_main.xml. */
    public static final String ARG_MEETING_ID = "meetingId";

    private final GetMeetingDetailUseCase getMeetingDetailUseCase;
    private final SessionRepository sessionRepository;
    private final Executor mainExecutor;
    private final String meetingId;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<MeetingDetailUiState> state =
            new MutableLiveData<>(MeetingDetailUiState.loading());

    @Inject
    public MeetingDetailViewModel(
            SavedStateHandle savedStateHandle,
            GetMeetingDetailUseCase getMeetingDetailUseCase,
            SessionRepository sessionRepository,
            @MainExecutor Executor mainExecutor) {
        this.getMeetingDetailUseCase = getMeetingDetailUseCase;
        this.sessionRepository = sessionRepository;
        this.mainExecutor = mainExecutor;
        this.meetingId = savedStateHandle.get(ARG_MEETING_ID);

        load();
    }

    public LiveData<MeetingDetailUiState> getState() {
        return state;
    }

    /** Trigger (re)loading the meeting detail. Safe to call on retry. */
    public void load() {
        if (meetingId == null || meetingId.isBlank()) {
            state.setValue(MeetingDetailUiState.error("Missing meeting id"));
            return;
        }
        String userId = currentUserId();
        if (userId == null) {
            state.setValue(MeetingDetailUiState.error("Session expired. Please sign in again."));
            return;
        }

        state.setValue(MeetingDetailUiState.loading());
        CompletableFuture<?> future = getMeetingDetailUseCase
                .execute(userId, meetingId)
                .thenAcceptAsync(
                        detail -> state.setValue(MeetingDetailUiState.success(detail)),
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(
                            () -> state.setValue(MeetingDetailUiState.error(rootMessage(error))));
                    return null;
                });
        trackFuture(future);
    }

    /** Tracks a future for cancellation when ViewModel is cleared. */
    private void trackFuture(CompletableFuture<?> future) {
        activeFutures.add(future);
        future.whenComplete((result, ex) -> activeFutures.remove(future));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (CompletableFuture<?> f : activeFutures) {
            if (!f.isDone()) {
                f.cancel(true);
            }
        }
        activeFutures.clear();
    }

    private String currentUserId() {
        SessionInfo session = sessionRepository.getSession();
        return session != null ? session.userId() : null;
    }

    private String rootMessage(Throwable error) {
        Throwable cause = error.getCause() != null ? error.getCause() : error;
        String message = cause.getMessage();
        return message != null ? message : "Something went wrong";
    }
}
