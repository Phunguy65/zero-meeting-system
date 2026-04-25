package io.github.phunguy65.zms.presentation.videocall;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.repository.ChatDataMessageHandler;
import io.github.phunguy65.zms.di.LiveKitUrl;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import io.github.phunguy65.zms.domain.model.JoinRoomResult;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.RoomConnectionState;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.VideoLayout;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository;
import io.github.phunguy65.zms.domain.repository.LiveKitRepository;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import io.github.phunguy65.zms.domain.repository.ParticipantRepository;
import io.github.phunguy65.zms.domain.repository.RecordingRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.repository.WaitingRoomRepository;
import io.livekit.android.room.track.LocalVideoTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.inject.Inject;

/**
 * Shared ViewModel for video call flow, scoped to VideoCallActivity.
 * Manages call state across PreJoinFragment and ActiveCallFragment.
 * Orchestrates join approval, room connection, participant state,
 * layout selection, and host meeting settings.
 */
@HiltViewModel
public class CallViewModel extends ViewModel {

    private final LiveKitRepository liveKitRepository;
    private final JoinRoomRepository joinRoomRepository;
    private final SessionRepository sessionRepository;
    private final MeetingRepository meetingRepository;
    private final WaitingRoomRepository waitingRoomRepository;
    private final ParticipantRepository participantRepository;
    private final RecordingRepository recordingRepository;
    private final String liveKitUrl;
    private final ChatDataMessageHandler chatDataMessageHandler;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> _isMicEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> _isCameraEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> _meetingCode = new MutableLiveData<>("");
    private final MutableLiveData<String> _displayName = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _isGuest = new MutableLiveData<>(false);

    private final MutableLiveData<JoinState> _joinState = new MutableLiveData<>(JoinState.IDLE);
    private final MutableLiveData<String> _joinError = new MutableLiveData<>(null);
    private final MutableLiveData<String> _livekitToken = new MutableLiveData<>(null);

    private final MutableLiveData<RoomConnectionState> _connectionState =
            new MutableLiveData<>(RoomConnectionState.DISCONNECTED);
    private final MutableLiveData<List<VideoParticipant>> _participants =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<LocalVideoTrack> _localVideoTrack = new MutableLiveData<>(null);
    private final MutableLiveData<List<String>> _activeSpeakers =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<VideoLayout> _currentLayout =
            new MutableLiveData<>(VideoLayout.AUTO);

    private final MutableLiveData<Boolean> _isHost = new MutableLiveData<>(false);
    private final MutableLiveData<String> _meetingId = new MutableLiveData<>(null);
    private final MutableLiveData<MeetingSettings> _meetingSettings = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _isSettingsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _settingsError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _settingsUpdateSuccess = new MutableLiveData<>(false);

    private final MutableLiveData<List<JoinRequestItem>> _pendingJoinRequests =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> _pendingCount = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> _isWaitingRoomSseConnected =
            new MutableLiveData<>(false);
    private final MutableLiveData<String> _participantKickedEvent = new MutableLiveData<>(null);

    private final MutableLiveData<Boolean> _isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _isRecordingLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _recordingError = new MutableLiveData<>(null);

    private static final long RECONNECT_INITIAL_DELAY_MS = 1000;
    private static final long RECONNECT_MAX_DELAY_MS = 30000;
    private long currentReconnectDelay = RECONNECT_INITIAL_DELAY_MS;
    private Handler reconnectHandler;
    private Runnable reconnectRunnable;
    private boolean waitingRoomSseActive = false;

    private String meetingUuid;

    private final MutableLiveData<Long> _callDuration = new MutableLiveData<>(0L);
    private long callStartTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    private String deviceId;

    private final MutableLiveData<Boolean> _requiresPassword = new MutableLiveData<>(false);
    private final MutableLiveData<String> _password = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _isFetchingMeetingInfo = new MutableLiveData<>(false);
    private final MutableLiveData<String> _fetchError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _readyToJoin = new MutableLiveData<>(false);
    private final MutableLiveData<JoinRoomResult.DenyReasonCode> _denyReasonCode =
            new MutableLiveData<>(null);

    /**
     * Represents the join request state machine.
     */
    public enum JoinState {
        IDLE,
        REQUESTING,
        WAITING_APPROVAL,
        APPROVED,
        DENIED,
        EXPIRED,
        ERROR
    }

    @Inject
    public CallViewModel(
            LiveKitRepository liveKitRepository,
            JoinRoomRepository joinRoomRepository,
            ChatDataMessageHandler chatDataMessageHandler,
            SessionRepository sessionRepository,
            MeetingRepository meetingRepository,
            WaitingRoomRepository waitingRoomRepository,
            ParticipantRepository participantRepository,
            RecordingRepository recordingRepository,
            @LiveKitUrl String liveKitUrl,
            @MainExecutor Executor mainExecutor) {
        this.liveKitRepository = liveKitRepository;
        this.joinRoomRepository = joinRoomRepository;
        this.chatDataMessageHandler = chatDataMessageHandler;
        this.sessionRepository = sessionRepository;
        this.meetingRepository = meetingRepository;
        this.waitingRoomRepository = waitingRoomRepository;
        this.participantRepository = participantRepository;
        this.recordingRepository = recordingRepository;
        this.liveKitUrl = liveKitUrl;
        this.mainExecutor = mainExecutor;

        liveKitRepository.setRoomEventListener(new RoomEventListenerImpl());
    }

    public LiveData<Boolean> isMicEnabled() {
        return _isMicEnabled;
    }

    public LiveData<Boolean> isCameraEnabled() {
        return _isCameraEnabled;
    }

    public LiveData<String> getMeetingCode() {
        return _meetingCode;
    }

    public LiveData<String> getDisplayName() {
        return _displayName;
    }

    public LiveData<Boolean> isGuest() {
        return _isGuest;
    }

    public LiveData<Long> getCallDuration() {
        return _callDuration;
    }

    public LiveData<JoinState> getJoinState() {
        return _joinState;
    }

    public LiveData<String> getJoinError() {
        return _joinError;
    }

    public LiveData<RoomConnectionState> getConnectionState() {
        return _connectionState;
    }

    public LiveData<List<VideoParticipant>> getParticipants() {
        return _participants;
    }

    public LiveData<LocalVideoTrack> getLocalVideoTrack() {
        return _localVideoTrack;
    }

    public LiveData<List<String>> getActiveSpeakers() {
        return _activeSpeakers;
    }

    public LiveData<String> getLivekitToken() {
        return _livekitToken;
    }

    public LiveData<VideoLayout> getCurrentLayout() {
        return _currentLayout;
    }

    public LiveData<Boolean> isHost() {
        return _isHost;
    }

    public LiveData<String> getMeetingId() {
        return _meetingId;
    }

    public LiveData<MeetingSettings> getMeetingSettings() {
        return _meetingSettings;
    }

    public LiveData<Boolean> isSettingsLoading() {
        return _isSettingsLoading;
    }

    public LiveData<String> getSettingsError() {
        return _settingsError;
    }

    public LiveData<Boolean> getSettingsUpdateSuccess() {
        return _settingsUpdateSuccess;
    }

    public void clearSettingsUpdateSuccess() {
        _settingsUpdateSuccess.setValue(false);
    }

    public LiveData<List<JoinRequestItem>> getPendingJoinRequests() {
        return _pendingJoinRequests;
    }

    public LiveData<Integer> getPendingCount() {
        return _pendingCount;
    }

    public LiveData<Boolean> isWaitingRoomSseConnected() {
        return _isWaitingRoomSseConnected;
    }

    /**
     * LiveData event carrying the display name of the most recently kicked participant.
     * Emits null when cleared.
     */
    public LiveData<String> getParticipantKickedEvent() {
        return _participantKickedEvent;
    }

    public void clearParticipantKickedEvent() {
        _participantKickedEvent.setValue(null);
    }

    /**
     * LiveData indicating whether the room is actively being recorded,
     * driven by room metadata changes.
     */
    public LiveData<Boolean> isRecording() {
        return _isRecording;
    }

    /**
     * LiveData indicating whether a recording start/stop request is in flight.
     */
    public LiveData<Boolean> isRecordingLoading() {
        return _isRecordingLoading;
    }

    /**
     * One-shot LiveData carrying an error message from a failed recording action.
     * Consumers should clear after displaying.
     */
    public LiveData<String> getRecordingError() {
        return _recordingError;
    }

    /**
     * Clears the recording error so it is only consumed once.
     */
    public void clearRecordingError() {
        _recordingError.setValue(null);
    }

    public LiveData<Boolean> requiresPassword() {
        return _requiresPassword;
    }

    public LiveData<String> getPassword() {
        return _password;
    }

    public LiveData<Boolean> isFetchingMeetingInfo() {
        return _isFetchingMeetingInfo;
    }

    public LiveData<String> getFetchError() {
        return _fetchError;
    }

    public LiveData<Boolean> isReadyToJoin() {
        return _readyToJoin;
    }

    public void clearReadyToJoin() {
        _readyToJoin.setValue(false);
    }

    public LiveData<JoinRoomResult.DenyReasonCode> getDenyReasonCode() {
        return _denyReasonCode;
    }

    public void setPassword(String password) {
        _password.setValue(password);
    }

    public void setMicEnabled(boolean enabled) {
        _isMicEnabled.setValue(enabled);
    }

    public void setCameraEnabled(boolean enabled) {
        _isCameraEnabled.setValue(enabled);
    }

    /**
     * Sets the meeting code for join requests.
     * Clears cached meetingUuid and password-related state if code changes,
     * forcing fresh resolution during join.
     */
    public void setMeetingCode(String code) {
        String current = _meetingCode.getValue();
        if (current != null && !current.equals(code)) {
            this.meetingUuid = null;
            _password.setValue("");
            _requiresPassword.setValue(false);
            _fetchError.setValue(null);
        }
        _meetingCode.setValue(code);
    }

    public void setDisplayName(String name) {
        _displayName.setValue(name);
    }

    public void setIsGuest(boolean isGuest) {
        _isGuest.setValue(isGuest);
    }

    /**
     * Returns the current guest status value synchronously.
     * @return true if this is a guest session
     */
    public boolean isGuestValue() {
        Boolean guest = _isGuest.getValue();
        return guest != null && guest;
    }

    /**
     * Loads the display name from the session repository for authenticated users.
     * Should be called when initializing for non-guest users.
     */
    public void loadUserDisplayName() {
        SessionInfo session = sessionRepository.getSession();
        if (session != null && session.fullName() != null && !session.fullName().isEmpty()) {
            _displayName.setValue(session.fullName());
        }
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Sets the meeting UUID for API calls (getMeetingDetail, updateMeetingSettings).
     * This should be the actual meeting UUID, not the shortCode.
     * When joining from Dashboard with an upcoming meeting, this is set directly.
     * When joining via manual code entry, this will be null initially.
     */
    public void setMeetingUuid(String uuid) {
        this.meetingUuid = uuid;
    }

    /**
     * Sets the current video layout mode.
     * Notifies observers to update the participant grid arrangement.
     */
    public void setCurrentLayout(VideoLayout layout) {
        _currentLayout.setValue(layout);
    }

    /**
     * Sets the meeting ID for the current call.
     * Used for loading/updating meeting settings.
     */
    public void setMeetingId(String meetingId) {
        _meetingId.setValue(meetingId);
    }

    /**
     * Sets whether the current user is the meeting host.
     */
    public void setIsHost(boolean isHost) {
        _isHost.setValue(isHost);
    }

    /**
     * Loads meeting settings from the server.
     * Only available to the meeting host.
     */
    public void loadMeetingSettings() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        _isSettingsLoading.setValue(true);
        _settingsError.setValue(null);

        meetingRepository
                .getMeetingDetail(meetingId)
                .whenCompleteAsync(
                        (detail, error) -> {
                            _isSettingsLoading.postValue(false);

                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _settingsError.postValue(errorMessage);
                                return;
                            }

                            _meetingSettings.postValue(detail.settings());
                        },
                        mainExecutor);
    }

    /**
     * Updates meeting settings on the server.
     * Refreshes local state from the server response.
     */
    public void updateMeetingSettings(MeetingSettings settings) {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        _isSettingsLoading.setValue(true);
        _settingsError.setValue(null);
        _settingsUpdateSuccess.setValue(false);

        meetingRepository
                .updateMeetingSettings(meetingId, settings)
                .whenCompleteAsync(
                        (updatedSettings, error) -> {
                            _isSettingsLoading.postValue(false);

                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _settingsError.postValue(errorMessage);
                                return;
                            }

                            _meetingSettings.postValue(updatedSettings);
                            _settingsUpdateSuccess.postValue(true);
                        },
                        mainExecutor);
    }

    /**
     * Fetches meeting info by short code to determine if password is required.
     * If meeting does not require password, signals ready to join (fragment handles permissions).
     * If meeting requires password, reveals password field and waits for user input.
     *
     * @param shortCode the meeting short code to lookup
     */
    public void fetchMeetingInfoAndJoin(String shortCode) {
        if (shortCode == null || shortCode.isEmpty()) {
            _fetchError.setValue("Meeting code is required");
            return;
        }

        _isFetchingMeetingInfo.setValue(true);
        _fetchError.setValue(null);
        _readyToJoin.setValue(false);

        meetingRepository
                .getMeetingByShortCode(shortCode)
                .whenCompleteAsync(
                        (detail, error) -> {
                            _isFetchingMeetingInfo.postValue(false);

                            if (error != null) {
                                Throwable cause =
                                        error.getCause() != null ? error.getCause() : error;
                                String errorMessage = cause.getMessage();
                                _fetchError.postValue(errorMessage);
                                return;
                            }

                            this.meetingUuid = detail.id();
                            boolean needsPassword = detail.settings() != null
                                    && detail.settings().isRequirePassword();

                            _requiresPassword.postValue(needsPassword);

                            if (!needsPassword) {
                                _readyToJoin.postValue(true);
                            }
                        },
                        mainExecutor);
    }

    /**
     * Initiates a join request to the backend.
     * Handles APPROVED and PENDING responses appropriately.
     * Uses meetingUuid when available (from Dashboard/CreateMeeting flows),
     * otherwise resolves shortCode via API lookup.
     * Passes current password for protected meetings.
     */
    public void requestJoinRoom() {
        String code = _meetingCode.getValue();
        String name = _displayName.getValue();
        String password = _password.getValue();

        if (code == null || code.isEmpty()) {
            _joinError.setValue("Meeting code is required");
            return;
        }

        _joinState.setValue(JoinState.REQUESTING);
        _joinError.setValue(null);

        String passwordToSend = (password != null && !password.isEmpty()) ? password : null;

        joinRoomRepository
                .requestJoin(code, meetingUuid, name != null ? name : "", deviceId, passwordToSend)
                .whenCompleteAsync(
                        (result, error) -> {
                            if (error != null) {
                                _joinState.postValue(JoinState.ERROR);
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _joinError.postValue(errorMessage);
                                return;
                            }

                            handleJoinResult(result);
                        },
                        mainExecutor);
    }

    /**
     * Handles the join request result from the backend.
     * Updates meeting UUID from resolved value if available.
     */
    private void handleJoinResult(JoinRoomResult result) {
        String resolvedUuid = result.getMeetingUuid();
        if (resolvedUuid != null && !resolvedUuid.isEmpty()) {
            this.meetingUuid = resolvedUuid;
        }

        switch (result.getStatus()) {
            case APPROVED:
                _joinState.setValue(JoinState.APPROVED);
                _livekitToken.setValue(result.getLivekitToken());
                initializeMeetingContext();
                break;

            case PENDING:
                _joinState.setValue(JoinState.WAITING_APPROVAL);
                subscribeToApprovalEvents(result.getRequestId());
                break;

            case DENIED:
                _joinState.setValue(JoinState.DENIED);
                _denyReasonCode.setValue(result.getDenyReasonCode());
                _joinError.setValue(result.getDenyReason());
                break;
        }
    }

    /**
     * Initializes meeting context (ID and host status) after successful join approval.
     * Uses the meeting UUID which is either set from intent extras or resolved from API.
     * The meeting UUID is required for API calls (getMeetingDetail, updateMeetingSettings).
     */
    private void initializeMeetingContext() {
        String effectiveMeetingId = meetingUuid;
        if (effectiveMeetingId == null || effectiveMeetingId.isEmpty()) {
            effectiveMeetingId = _meetingCode.getValue();
        }
        if (effectiveMeetingId != null && !effectiveMeetingId.isEmpty()) {
            _meetingId.setValue(effectiveMeetingId);
            fetchHostStatus(effectiveMeetingId);
        }
    }

    /**
     * Fetches meeting detail to determine if current user is the host.
     * When user is host and waiting room is enabled, starts the waiting room SSE.
     */
    private void fetchHostStatus(String meetingId) {
        meetingRepository
                .getMeetingDetail(meetingId)
                .whenCompleteAsync(
                        (detail, error) -> {
                            if (error != null) {
                                _isHost.postValue(false);
                                return;
                            }

                            SessionInfo session = sessionRepository.getSession();
                            if (session != null && detail != null) {
                                String currentUserId = session.userId();
                                String hostId = detail.hostId();
                                boolean isHost =
                                        currentUserId != null && currentUserId.equals(hostId);
                                _isHost.postValue(isHost);

                                if (detail.settings() != null) {
                                    _meetingSettings.postValue(detail.settings());
                                }

                                if (isHost
                                        && detail.settings() != null
                                        && detail.settings().isWaitingRoomEnabled()) {
                                    startWaitingRoomSse();
                                }
                            } else {
                                _isHost.postValue(false);
                            }
                        },
                        mainExecutor);
    }

    /**
     * Subscribes to SSE approval events for pending requests.
     */
    private void subscribeToApprovalEvents(String requestId) {
        joinRoomRepository.subscribeToApproval(
                requestId, new JoinRoomRepository.ApprovalEventListener() {
                    @Override
                    public void onApproved(String livekitToken) {
                        mainExecutor.execute(() -> {
                            _joinState.setValue(JoinState.APPROVED);
                            _livekitToken.setValue(livekitToken);
                            initializeMeetingContext();
                        });
                    }

                    @Override
                    public void onDenied(String reason) {
                        mainExecutor.execute(() -> {
                            _joinState.setValue(JoinState.DENIED);
                            _joinError.setValue(reason);
                        });
                    }

                    @Override
                    public void onExpired() {
                        mainExecutor.execute(() -> {
                            _joinState.setValue(JoinState.EXPIRED);
                            _joinError.setValue(null);
                        });
                    }

                    @Override
                    public void onError(String message) {
                        mainExecutor.execute(() -> {
                            _joinState.setValue(JoinState.ERROR);
                            _joinError.setValue(message);
                        });
                    }
                });
    }

    /**
     * Cancels any pending approval subscription.
     */
    public void cancelJoinRequest() {
        joinRoomRepository.cancelApprovalSubscription();
        _joinState.setValue(JoinState.IDLE);
    }

    /**
     * Resets join state to allow retry.
     * Clears password-related state, cached lookup errors, and password-required flags.
     */
    public void resetJoinState() {
        _joinState.setValue(JoinState.IDLE);
        _joinError.setValue(null);
        _denyReasonCode.setValue(null);
        _password.setValue("");
        _requiresPassword.setValue(false);
        _fetchError.setValue(null);
        _isFetchingMeetingInfo.setValue(false);
        _readyToJoin.setValue(false);
    }

    /**
     * Connects to the LiveKit room using the stored token.
     */
    public void connectToRoom() {
        String token = _livekitToken.getValue();
        if (token == null || token.isEmpty()) {
            return;
        }
        connectToRoom(liveKitUrl, token);
    }

    /**
     * Connects to a LiveKit room with the given URL and token.
     * Passes current pre-join mic/cam states to the repository for post-connect application.
     */
    public void connectToRoom(String url, String token) {
        Boolean micEnabled = _isMicEnabled.getValue();
        Boolean cameraEnabled = _isCameraEnabled.getValue();

        liveKitRepository.connect(
                url,
                token,
                micEnabled != null && micEnabled,
                cameraEnabled != null && cameraEnabled);
    }

    /**
     * Toggles the local microphone and syncs with LiveKit.
     */
    public void toggleLocalMic() {
        Boolean current = _isMicEnabled.getValue();
        boolean newState = current == null || !current;
        _isMicEnabled.setValue(newState);
        liveKitRepository.setMicrophoneEnabled(newState);
    }

    /**
     * Toggles the local camera and syncs with LiveKit.
     */
    public void toggleLocalCamera() {
        Boolean current = _isCameraEnabled.getValue();
        boolean newState = current == null || !current;
        _isCameraEnabled.setValue(newState);
        liveKitRepository.setCameraEnabled(newState);
    }

    /**
     * Switches between front and back camera.
     */
    public void switchCamera() {
        liveKitRepository.switchCamera();
    }

    /**
     * Starts the host waiting room SSE subscription.
     * Subscribes to meeting events for join request notifications.
     */
    public void startWaitingRoomSse() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) return;
        if (waitingRoomSseActive) return;

        waitingRoomSseActive = true;
        currentReconnectDelay = RECONNECT_INITIAL_DELAY_MS;
        String authToken = sessionRepository.getAccessToken();

        waitingRoomRepository.subscribeToHostEvents(
                meetingId, authToken, new WaitingRoomRepository.HostEventListener() {
                    @Override
                    public void onConnected() {
                        mainExecutor.execute(() -> {
                            _isWaitingRoomSseConnected.setValue(true);
                            currentReconnectDelay = RECONNECT_INITIAL_DELAY_MS;
                        });
                    }

                    @Override
                    public void onJoinRequestCreated(
                            String requestId, String eventMeetingId, String displayName) {
                        mainExecutor.execute(() -> {
                            _isWaitingRoomSseConnected.setValue(true);
                            currentReconnectDelay = RECONNECT_INITIAL_DELAY_MS;

                            List<JoinRequestItem> current = _pendingJoinRequests.getValue();
                            List<JoinRequestItem> updated =
                                    current != null ? new ArrayList<>(current) : new ArrayList<>();

                            boolean alreadyExists = false;
                            for (JoinRequestItem existing : updated) {
                                if (existing.getId().equals(requestId)) {
                                    alreadyExists = true;
                                    break;
                                }
                            }

                            if (!alreadyExists) {
                                updated.add(new JoinRequestItem(
                                        requestId, eventMeetingId, displayName, ""));
                                _pendingJoinRequests.setValue(updated);
                                _pendingCount.setValue(updated.size());
                            }
                        });
                    }

                    @Override
                    public void onJoinRequestExpired(String requestId) {
                        mainExecutor.execute(() -> {
                            List<JoinRequestItem> current = _pendingJoinRequests.getValue();
                            if (current == null) return;
                            List<JoinRequestItem> updated = new ArrayList<>();
                            for (JoinRequestItem item : current) {
                                if (!item.getId().equals(requestId)) {
                                    updated.add(item);
                                }
                            }
                            _pendingJoinRequests.setValue(updated);
                            _pendingCount.setValue(updated.size());
                        });
                    }

                    @Override
                    public void onParticipantKicked(
                            String eventMeetingId, String kickedUserId, String displayName) {
                        mainExecutor.execute(() -> _participantKickedEvent.setValue(displayName));
                    }

                    @Override
                    public void onError(String message) {
                        mainExecutor.execute(() -> {
                            _isWaitingRoomSseConnected.setValue(false);
                            scheduleReconnect();
                        });
                    }
                });

        _isWaitingRoomSseConnected.setValue(true);
        syncPendingRequests();
    }

    /**
     * Stops the host waiting room SSE subscription and cancels any pending reconnect.
     */
    public void stopWaitingRoomSse() {
        waitingRoomSseActive = false;
        waitingRoomRepository.cancelHostSubscription();
        _isWaitingRoomSseConnected.setValue(false);
        cancelReconnect();
    }

    /**
     * Schedules a reconnect attempt using exponential backoff.
     * Delay progression: 1s, 2s, 4s, 8s, 16s, capped at 30s.
     */
    private void scheduleReconnect() {
        if (!waitingRoomSseActive) return;

        cancelReconnect();

        if (reconnectHandler == null) {
            reconnectHandler = new Handler(Looper.getMainLooper());
        }

        reconnectRunnable = () -> {
            if (!waitingRoomSseActive) return;
            waitingRoomRepository.cancelHostSubscription();
            waitingRoomSseActive = false;
            startWaitingRoomSse();
        };

        reconnectHandler.postDelayed(reconnectRunnable, currentReconnectDelay);
        currentReconnectDelay = Math.min(currentReconnectDelay * 2, RECONNECT_MAX_DELAY_MS);
    }

    private void cancelReconnect() {
        if (reconnectHandler != null && reconnectRunnable != null) {
            reconnectHandler.removeCallbacks(reconnectRunnable);
            reconnectRunnable = null;
        }
    }

    /**
     * Fetches the full pending join request list from the API and merges with local state.
     * Preserves SSE-added entries and only appends API items not already present locally.
     * Called after successful SSE reconnect to repair any missed events.
     */
    public void syncPendingRequests() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) return;

        waitingRoomRepository
                .listPendingRequests(meetingId)
                .whenCompleteAsync(
                        (requests, error) -> {
                            if (error != null) return;

                            List<JoinRequestItem> local = _pendingJoinRequests.getValue();
                            List<JoinRequestItem> merged =
                                    local != null ? new ArrayList<>(local) : new ArrayList<>();

                            Set<String> existingIds = new java.util.HashSet<>();
                            for (JoinRequestItem item : merged) {
                                existingIds.add(item.getId());
                            }

                            for (JoinRequestItem apiItem : requests) {
                                if (!existingIds.contains(apiItem.getId())) {
                                    merged.add(apiItem);
                                }
                            }

                            _pendingJoinRequests.postValue(merged);
                            _pendingCount.postValue(merged.size());
                        },
                        mainExecutor);
    }

    /**
     * Removes a pending request from local state after external moderation
     * (approve or deny) so the toolbar badge stays in sync.
     *
     * @param requestId the id of the moderated request
     */
    public void removePendingRequest(String requestId) {
        List<JoinRequestItem> current = _pendingJoinRequests.getValue();
        if (current == null) return;
        List<JoinRequestItem> updated = new ArrayList<>();
        for (JoinRequestItem item : current) {
            if (!item.getId().equals(requestId)) {
                updated.add(item);
            }
        }
        _pendingJoinRequests.setValue(updated);
        _pendingCount.setValue(updated.size());
    }

    /**
     * Clears all pending requests from local state after an approve-all action.
     */
    public void clearAllPendingRequests() {
        _pendingJoinRequests.setValue(new ArrayList<>());
        _pendingCount.setValue(0);
    }

    /**
     * Ends the call and disconnects from the room.
     */
    public void endCall() {
        stopCallTimer();
        stopWaitingRoomSse();
        liveKitRepository.disconnect();
        joinRoomRepository.cancelApprovalSubscription();
        _connectionState.setValue(RoomConnectionState.DISCONNECTED);
        _participants.setValue(new ArrayList<>());
        _localVideoTrack.setValue(null);
    }

    public void toggleMic() {
        toggleLocalMic();
    }

    public void toggleCamera() {
        toggleLocalCamera();
    }

    /**
     * Starts the call timer. Should be called once when entering ActiveCallFragment.
     * Timer survives configuration changes since it's managed by ViewModel.
     */
    public void startCallTimer() {
        if (isTimerRunning) {
            return; // Already running, don't restart
        }

        callStartTime = System.currentTimeMillis();
        isTimerRunning = true;
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    long elapsedSeconds = (System.currentTimeMillis() - callStartTime) / 1000;
                    _callDuration.setValue(elapsedSeconds);
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    /**
     * Stops the call timer.
     */
    public void stopCallTimer() {
        isTimerRunning = false;
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    /**
     * Validates pre-join form.
     * @return true if valid, false otherwise
     */
    public boolean validatePreJoin() {
        String code = _meetingCode.getValue();
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        Boolean isGuestMode = _isGuest.getValue();
        if (Boolean.TRUE.equals(isGuestMode)) {
            String name = _displayName.getValue();
            return name != null && !name.trim().isEmpty();
        }

        return true;
    }

    /**
     * Mutes all participants' microphones (host action).
     * Executes asynchronously; surfaces errors via {@code _settingsError}.
     */
    public void muteAllParticipants() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        participantRepository
                .muteAll(meetingId)
                .whenCompleteAsync(
                        (unused, error) -> {
                            if (error != null) {
                                Throwable cause =
                                        error.getCause() != null ? error.getCause() : error;
                                _settingsError.postValue(cause.getMessage());
                            }
                        },
                        mainExecutor);
    }

    /**
     * Mutes a specific participant's track (host action).
     * Executes asynchronously; surfaces errors via {@code _settingsError}.
     *
     * @param identity the LiveKit participant identity
     * @param source   the track source: {@code "microphone"} or {@code "camera"}
     */
    public void muteParticipantTrack(String identity, String source) {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        participantRepository
                .muteTrack(meetingId, identity, source)
                .whenCompleteAsync(
                        (unused, error) -> {
                            if (error != null) {
                                Throwable cause =
                                        error.getCause() != null ? error.getCause() : error;
                                _settingsError.postValue(cause.getMessage());
                            }
                        },
                        mainExecutor);
    }

    /**
     * Starts recording for the current meeting.
     * Sets loading state during the request and maps errors to recording error LiveData.
     */
    public void startRecording() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        Boolean loading = _isRecordingLoading.getValue();
        if (loading != null && loading) {
            return;
        }

        _isRecordingLoading.setValue(true);
        _recordingError.setValue(null);

        recordingRepository
                .startRecording(meetingId)
                .whenCompleteAsync(
                        (unused, error) -> {
                            _isRecordingLoading.postValue(false);

                            if (error != null) {
                                Throwable cause =
                                        error.getCause() != null ? error.getCause() : error;
                                _recordingError.postValue(cause.getMessage());
                            }
                        },
                        mainExecutor);
    }

    /**
     * Stops recording for the current meeting.
     * Sets loading state during the request. On failure, the control remains
     * in active recording state so the host can retry.
     */
    public void stopRecording() {
        String meetingId = _meetingId.getValue();
        if (meetingId == null || meetingId.isEmpty()) {
            return;
        }

        Boolean loading = _isRecordingLoading.getValue();
        if (loading != null && loading) {
            return;
        }

        _isRecordingLoading.setValue(true);
        _recordingError.setValue(null);

        recordingRepository
                .stopRecording(meetingId)
                .whenCompleteAsync(
                        (unused, error) -> {
                            _isRecordingLoading.postValue(false);

                            if (error != null) {
                                Throwable cause =
                                        error.getCause() != null ? error.getCause() : error;
                                _recordingError.postValue(cause.getMessage());
                            }
                        },
                        mainExecutor);
    }

    /**
     * Toggles recording based on current state. Starts if inactive, stops if active.
     */
    public void toggleRecording() {
        Boolean recording = _isRecording.getValue();
        if (recording != null && recording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    /**
     * Parses room metadata JSON and updates recording state.
     * Malformed or empty metadata is treated as recording inactive.
     *
     * @param metadata the raw room metadata string
     */
    private void handleRoomMetadataChanged(String metadata) {
        boolean recording = false;

        if (metadata != null && !metadata.isEmpty()) {
            try {
                org.json.JSONObject json = new org.json.JSONObject(metadata);
                recording = json.optBoolean("recording", false);
            } catch (org.json.JSONException e) {
                recording = false;
            }
        }

        _isRecording.postValue(recording);

        if (!recording) {
            _isRecordingLoading.postValue(false);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopCallTimer();
        stopWaitingRoomSse();
        liveKitRepository.removeRoomEventListener();
        liveKitRepository.disconnect();
        joinRoomRepository.cancelApprovalSubscription();
    }

    private class RoomEventListenerImpl implements LiveKitRepository.RoomEventListener {

        @Override
        public void onConnectionStateChanged(RoomConnectionState state) {
            _connectionState.postValue(state);
        }

        @Override
        public void onParticipantConnected(VideoParticipant participant) {
            // Participants list is updated via onParticipantsUpdated
        }

        @Override
        public void onDataReceived(byte[] data) {
            chatDataMessageHandler.handleDataReceived(data);
        }

        @Override
        public void onParticipantDisconnected(String participantId) {
            // Participants list is updated via onParticipantsUpdated
        }

        @Override
        public void onParticipantsUpdated(List<VideoParticipant> participants) {
            _participants.postValue(new ArrayList<>(participants));
        }

        @Override
        public void onActiveSpeakersChanged(List<String> speakerIds) {
            _activeSpeakers.postValue(new ArrayList<>(speakerIds));
        }

        @Override
        public void onLocalVideoTrackAvailable(LocalVideoTrack track) {
            _localVideoTrack.postValue(track);
        }

        @Override
        public void onRoomMetadataChanged(String metadata) {
            handleRoomMetadataChanged(metadata);
        }
    }
}
