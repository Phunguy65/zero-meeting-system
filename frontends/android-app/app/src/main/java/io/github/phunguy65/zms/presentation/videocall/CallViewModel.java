package io.github.phunguy65.zms.presentation.videocall;

import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.LiveKitUrl;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.JoinRoomResult;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.RoomConnectionState;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.VideoLayout;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.domain.repository.JoinRoomRepository;
import io.github.phunguy65.zms.domain.repository.LiveKitRepository;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.livekit.android.room.track.LocalVideoTrack;
import java.util.ArrayList;
import java.util.List;
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
    private final String liveKitUrl;
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
    private final MutableLiveData<List<String>> _activeSpeakers = new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<VideoLayout> _currentLayout = new MutableLiveData<>(VideoLayout.AUTO);

    private final MutableLiveData<Boolean> _isHost = new MutableLiveData<>(false);
    private final MutableLiveData<String> _meetingId = new MutableLiveData<>(null);
    private final MutableLiveData<MeetingSettings> _meetingSettings = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _isSettingsLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> _settingsError = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> _settingsUpdateSuccess = new MutableLiveData<>(false);

    private String meetingUuid;

    private final MutableLiveData<Long> _callDuration = new MutableLiveData<>(0L);
    private long callStartTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    private String deviceId;

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
            SessionRepository sessionRepository,
            MeetingRepository meetingRepository,
            @LiveKitUrl String liveKitUrl,
            @MainExecutor Executor mainExecutor) {
        this.liveKitRepository = liveKitRepository;
        this.joinRoomRepository = joinRoomRepository;
        this.sessionRepository = sessionRepository;
        this.meetingRepository = meetingRepository;
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

    public void setMicEnabled(boolean enabled) {
        _isMicEnabled.setValue(enabled);
    }

    public void setCameraEnabled(boolean enabled) {
        _isCameraEnabled.setValue(enabled);
    }

    /**
     * Sets the meeting code for join requests.
     * Clears cached meetingUuid if code differs from original,
     * forcing fresh resolution during join.
     */
    public void setMeetingCode(String code) {
        String current = _meetingCode.getValue();
        if (current != null && !current.equals(code)) {
            this.meetingUuid = null;
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

        meetingRepository.getMeetingDetail(meetingId)
                .whenCompleteAsync((detail, error) -> {
                    _isSettingsLoading.postValue(false);

                    if (error != null) {
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _settingsError.postValue(errorMessage);
                        return;
                    }

                    _meetingSettings.postValue(detail.settings());
                }, mainExecutor);
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

        meetingRepository.updateMeetingSettings(meetingId, settings)
                .whenCompleteAsync((updatedSettings, error) -> {
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
                }, mainExecutor);
    }


    /**
     * Initiates a join request to the backend.
     * Handles APPROVED and PENDING responses appropriately.
     * Uses meetingUuid when available (from Dashboard/CreateMeeting flows),
     * otherwise resolves shortCode via API lookup.
     */
    public void requestJoinRoom() {
        String code = _meetingCode.getValue();
        String name = _displayName.getValue();

        if (code == null || code.isEmpty()) {
            _joinError.setValue("Meeting code is required");
            return;
        }

        _joinState.setValue(JoinState.REQUESTING);
        _joinError.setValue(null);

        joinRoomRepository.requestJoin(code, meetingUuid, name != null ? name : "", deviceId)
                .whenCompleteAsync((result, error) -> {
                    if (error != null) {
                        _joinState.postValue(JoinState.ERROR);
                        String errorMessage = error.getCause() != null
                                ? error.getCause().getMessage()
                                : error.getMessage();
                        _joinError.postValue(errorMessage);
                        return;
                    }

                    handleJoinResult(result);
                }, mainExecutor);
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
     */
    private void fetchHostStatus(String meetingId) {
        meetingRepository.getMeetingDetail(meetingId)
                .whenCompleteAsync((detail, error) -> {
                    if (error != null) {
                        _isHost.postValue(false);
                        return;
                    }

                    SessionInfo session = sessionRepository.getSession();
                    if (session != null && detail != null) {
                        String currentUserId = session.userId();
                        String hostId = detail.hostId();
                        boolean isHost = currentUserId != null && currentUserId.equals(hostId);
                        _isHost.postValue(isHost);
                    } else {
                        _isHost.postValue(false);
                    }
                }, mainExecutor);
    }

    /**
     * Subscribes to SSE approval events for pending requests.
     */
    private void subscribeToApprovalEvents(String requestId) {
        joinRoomRepository.subscribeToApproval(requestId, new JoinRoomRepository.ApprovalEventListener() {
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
                    _joinError.setValue("Join request expired");
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
     */
    public void resetJoinState() {
        _joinState.setValue(JoinState.IDLE);
        _joinError.setValue(null);
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
     */
    public void connectToRoom(String url, String token) {
        Boolean micEnabled = _isMicEnabled.getValue();
        Boolean cameraEnabled = _isCameraEnabled.getValue();

        liveKitRepository.connect(url, token);

        liveKitRepository.setMicrophoneEnabled(micEnabled != null && micEnabled);
        liveKitRepository.setCameraEnabled(cameraEnabled != null && cameraEnabled);
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
     * Ends the call and disconnects from the room.
     */
    public void endCall() {
        stopCallTimer();
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
     * Mutes all participants (host action).
     */
    public void muteAllParticipants() {
        // TODO: Implement via LiveKit room API
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopCallTimer();
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
    }
}
