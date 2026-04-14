package io.github.phunguy65.zms.presentation.videocall;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

/**
 * Shared ViewModel for video call flow, scoped to VideoCallActivity.
 * Manages call state across PreJoinFragment and ActiveCallFragment.
 */
@HiltViewModel
public class CallViewModel extends ViewModel {

    private final MutableLiveData<Boolean> _isMicEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> _isCameraEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> _meetingCode = new MutableLiveData<>("");
    private final MutableLiveData<String> _displayName = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> _isGuest = new MutableLiveData<>(false);

    // Call duration timer (seconds) - managed by ViewModel to survive config changes
    private final MutableLiveData<Long> _callDuration = new MutableLiveData<>(0L);
    private long callStartTime = 0;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private boolean isTimerRunning = false;

    @Inject
    public CallViewModel() {
        // LiveKit repository will be injected here in the future
    }

    // ═══ Public LiveData (read-only) ═══

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

    // ═══ State Setters ═══

    public void setMicEnabled(boolean enabled) {
        _isMicEnabled.setValue(enabled);
    }

    public void setCameraEnabled(boolean enabled) {
        _isCameraEnabled.setValue(enabled);
    }

    public void setMeetingCode(String code) {
        _meetingCode.setValue(code);
    }

    public void setDisplayName(String name) {
        _displayName.setValue(name);
    }

    public void setIsGuest(boolean isGuest) {
        _isGuest.setValue(isGuest);
    }

    // ═══ Actions ═══

    public void toggleMic() {
        Boolean current = _isMicEnabled.getValue();
        _isMicEnabled.setValue(current == null || !current);
    }

    public void toggleCamera() {
        Boolean current = _isCameraEnabled.getValue();
        _isCameraEnabled.setValue(current == null || !current);
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
     * Ends the call and cleans up resources.
     * Will disconnect from LiveKit room when integrated.
     */
    public void endCall() {
        stopCallTimer();
        // TODO: Disconnect from LiveKit room
        // TODO: Release camera/mic resources
    }

    /**
     * Mutes all participants (host action).
     */
    public void muteAllParticipants() {
        // TODO: Implement when LiveKit is integrated
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopCallTimer();
    }
}
