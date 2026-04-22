package io.github.phunguy65.zms.presentation.videocall;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import io.github.phunguy65.zms.domain.repository.WaitingRoomRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import io.github.phunguy65.zms.di.MainExecutor;

/**
 * ViewModel for the waiting room bottom sheet.
 * Manages loading, error, empty, and has-items states
 * for the pending join request list with moderation actions.
 */
@HiltViewModel
public class WaitingRoomViewModel extends ViewModel {

    private final WaitingRoomRepository waitingRoomRepository;
    private final Executor mainExecutor;

    private final MutableLiveData<List<JoinRequestItem>> _joinRequests =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _error = new MutableLiveData<>(null);
    private final MutableLiveData<String> _actionError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _isEmpty = new MutableLiveData<>(true);
    private final MutableLiveData<String> _approvedOrDeniedRequestId = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _approveAllSuccess = new MutableLiveData<>(false);

    @Inject
    public WaitingRoomViewModel(
            WaitingRoomRepository waitingRoomRepository,
            @MainExecutor Executor mainExecutor) {
        this.waitingRoomRepository = waitingRoomRepository;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<List<JoinRequestItem>> getJoinRequests() {
        return _joinRequests;
    }

    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    public LiveData<String> getError() {
        return _error;
    }

    public LiveData<String> getActionError() {
        return _actionError;
    }

    public void clearActionError() {
        _actionError.setValue(null);
    }

    public LiveData<Boolean> isEmpty() {
        return _isEmpty;
    }

    public LiveData<String> getApprovedOrDeniedRequestId() {
        return _approvedOrDeniedRequestId;
    }

    public LiveData<Boolean> getApproveAllSuccess() {
        return _approveAllSuccess;
    }

    public void clearApprovedOrDeniedRequestId() {
        _approvedOrDeniedRequestId.setValue(null);
    }

    public void clearApproveAllSuccess() {
        _approveAllSuccess.setValue(false);
    }

    /**
     * Loads pending join requests for the given meeting.
     */
    public void loadRequests(String meetingId) {
        _isLoading.setValue(true);
        _error.setValue(null);

        waitingRoomRepository.listPendingRequests(meetingId)
                .whenCompleteAsync((requests, error) -> {
                    _isLoading.postValue(false);

                    if (error != null) {
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _error.postValue(errorMessage);
                        return;
                    }

                    _joinRequests.postValue(requests);
                    _isEmpty.postValue(requests == null || requests.isEmpty());
                }, mainExecutor);
    }

    /**
     * Approves a single join request and removes it from the local list on success.
     */
    public void approveRequest(String meetingId, String requestId) {
        waitingRoomRepository.approveRequest(meetingId, requestId)
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _actionError.postValue(errorMessage);
                        return;
                    }

                    removeRequestFromList(requestId);
                    _approvedOrDeniedRequestId.postValue(requestId);
                }, mainExecutor);
    }

    /**
     * Denies a single join request and removes it from the local list on success.
     */
    public void denyRequest(String meetingId, String requestId) {
        waitingRoomRepository.denyRequest(meetingId, requestId)
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _actionError.postValue(errorMessage);
                        return;
                    }

                    removeRequestFromList(requestId);
                    _approvedOrDeniedRequestId.postValue(requestId);
                }, mainExecutor);
    }

    /**
     * Approves all pending join requests and clears the local list on success.
     */
    public void approveAll(String meetingId) {
        waitingRoomRepository.approveAll(meetingId)
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _actionError.postValue(errorMessage);
                        return;
                    }

                    _joinRequests.postValue(new ArrayList<>());
                    _isEmpty.postValue(true);
                    _approveAllSuccess.postValue(true);
                }, mainExecutor);
    }

    private void removeRequestFromList(String requestId) {
        List<JoinRequestItem> current = _joinRequests.getValue();
        if (current == null) return;

        List<JoinRequestItem> updated = new ArrayList<>();
        for (JoinRequestItem item : current) {
            if (!item.getId().equals(requestId)) {
                updated.add(item);
            }
        }
        _joinRequests.postValue(updated);
        _isEmpty.postValue(updated.isEmpty());
    }
}
