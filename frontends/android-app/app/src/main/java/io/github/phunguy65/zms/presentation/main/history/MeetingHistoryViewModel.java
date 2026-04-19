package io.github.phunguy65.zms.presentation.main.history;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.MeetingHistory;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.history.GetMeetingHistoryUseCase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * ViewModel driving the meeting history list screen.
 *
 * <p>Owns paginated meeting history state and exposes a sealed {@link MeetingHistoryUiState}
 * through LiveData. Handles initial load, pull-to-refresh, and infinite scroll pagination.
 */
@HiltViewModel
public class MeetingHistoryViewModel extends ViewModel {

    static final int PAGE_SIZE = 20;

    private final GetMeetingHistoryUseCase getMeetingHistoryUseCase;
    private final SessionRepository sessionRepository;
    private final Executor mainExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<MeetingHistoryUiState> state =
            new MutableLiveData<>(MeetingHistoryUiState.loading());
    private final MutableLiveData<String> pageErrorEvent = new MutableLiveData<>();

    @Inject
    public MeetingHistoryViewModel(
            GetMeetingHistoryUseCase getMeetingHistoryUseCase,
            SessionRepository sessionRepository,
            @MainExecutor Executor mainExecutor) {
        this.getMeetingHistoryUseCase = getMeetingHistoryUseCase;
        this.sessionRepository = sessionRepository;
        this.mainExecutor = mainExecutor;

        loadInitial();
    }

    public LiveData<MeetingHistoryUiState> getState() {
        return state;
    }

    /**
     * One-shot error messages for pagination failures. Consumers should show a Snackbar and then
     * acknowledge by calling {@link #consumePageError()}.
     */
    public LiveData<String> getPageErrorEvent() {
        return pageErrorEvent;
    }

    public void consumePageError() {
        pageErrorEvent.setValue(null);
    }

    /** Initial load or retry after a full-screen error. */
    public void loadInitial() {
        String userId = currentUserId();
        if (userId == null) {
            state.setValue(MeetingHistoryUiState.error("Session expired. Please sign in again."));
            return;
        }

        state.setValue(MeetingHistoryUiState.loading());
        CompletableFuture<?> future = getMeetingHistoryUseCase
                .execute(userId, PAGE_SIZE, null)
                .thenAcceptAsync(
                        page -> {
                            if (page.items().isEmpty()) {
                                state.setValue(MeetingHistoryUiState.empty());
                            } else {
                                state.setValue(new MeetingHistoryUiState.Success(
                                        page.items(),
                                        page.nextPageToken(),
                                        page.hasNext(),
                                        false,
                                        false));
                            }
                        },
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(
                            () -> state.setValue(MeetingHistoryUiState.error(rootMessage(error))));
                    return null;
                });
        trackFuture(future);
    }

    /** Pull-to-refresh — reloads from the first page while keeping any existing items visible. */
    public void refresh() {
        String userId = currentUserId();
        if (userId == null) {
            state.setValue(MeetingHistoryUiState.error("Session expired. Please sign in again."));
            return;
        }

        MeetingHistoryUiState current = state.getValue();
        if (current instanceof MeetingHistoryUiState.Success success) {
            state.setValue(new MeetingHistoryUiState.Success(
                    success.items(), success.nextPageToken(), success.hasMore(), false, true));
        }

        CompletableFuture<?> future = getMeetingHistoryUseCase
                .execute(userId, PAGE_SIZE, null)
                .thenAcceptAsync(
                        page -> {
                            if (page.items().isEmpty()) {
                                state.setValue(MeetingHistoryUiState.empty());
                            } else {
                                state.setValue(new MeetingHistoryUiState.Success(
                                        page.items(),
                                        page.nextPageToken(),
                                        page.hasNext(),
                                        false,
                                        false));
                            }
                        },
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> {
                        MeetingHistoryUiState s = state.getValue();
                        if (s instanceof MeetingHistoryUiState.Success success) {
                            state.setValue(new MeetingHistoryUiState.Success(
                                    success.items(),
                                    success.nextPageToken(),
                                    success.hasMore(),
                                    false,
                                    false));
                            pageErrorEvent.setValue(rootMessage(error));
                        } else {
                            state.setValue(MeetingHistoryUiState.error(rootMessage(error)));
                        }
                    });
                    return null;
                });
        trackFuture(future);
    }

    /**
     * Request the next page. No-op when not in Success state, when no more pages are available,
     * or when a pagination / refresh load is already in-flight.
     */
    public void loadMore() {
        MeetingHistoryUiState current = state.getValue();
        if (!(current instanceof MeetingHistoryUiState.Success success)) {
            return;
        }
        if (!success.hasMore() || success.isLoadingMore() || success.isRefreshing()) {
            return;
        }
        String userId = currentUserId();
        if (userId == null) {
            return;
        }

        state.setValue(new MeetingHistoryUiState.Success(
                success.items(), success.nextPageToken(), success.hasMore(), true, false));

        CompletableFuture<?> future = getMeetingHistoryUseCase
                .execute(userId, PAGE_SIZE, success.nextPageToken())
                .thenAcceptAsync(
                        page -> {
                            MeetingHistoryUiState s = state.getValue();
                            if (!(s instanceof MeetingHistoryUiState.Success currentSuccess)) {
                                return;
                            }
                            List<MeetingHistory> merged = new ArrayList<>(currentSuccess.items());
                            merged.addAll(page.items());
                            state.setValue(new MeetingHistoryUiState.Success(
                                    merged,
                                    page.nextPageToken(),
                                    page.hasNext(),
                                    false,
                                    currentSuccess.isRefreshing()));
                        },
                        mainExecutor)
                .exceptionally(error -> {
                    mainExecutor.execute(() -> {
                        MeetingHistoryUiState s = state.getValue();
                        if (s instanceof MeetingHistoryUiState.Success currentSuccess) {
                            state.setValue(new MeetingHistoryUiState.Success(
                                    currentSuccess.items(),
                                    currentSuccess.nextPageToken(),
                                    currentSuccess.hasMore(),
                                    false,
                                    currentSuccess.isRefreshing()));
                        }
                        pageErrorEvent.setValue(rootMessage(error));
                    });
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
