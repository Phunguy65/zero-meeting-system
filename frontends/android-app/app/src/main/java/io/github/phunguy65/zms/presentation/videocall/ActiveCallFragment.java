package io.github.phunguy65.zms.presentation.videocall;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.RoomConnectionState;
import io.github.phunguy65.zms.domain.model.VideoLayout;
import io.github.phunguy65.zms.domain.model.VideoParticipant;
import io.github.phunguy65.zms.frontends.R;
import io.livekit.android.renderer.SurfaceViewRenderer;
import io.livekit.android.room.track.LocalVideoTrack;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import livekit.org.webrtc.EglBase;

/**
 * Active video call fragment with RecyclerView-based video grid.
 * Features auto-hide controls, self-view preview, connection quality indicator,
 * compact control bar with overflow actions, and layout selection.
 */
@AndroidEntryPoint
public class ActiveCallFragment extends Fragment
        implements MeetingActionsBottomSheet.ActionsCallback {

    private static final int CONTROLS_AUTO_HIDE_DELAY_MS = 3000;

    private CallViewModel viewModel;
    private VideoGridAdapter videoGridAdapter;

    // Views
    private ConstraintLayout rootLayout;
    private View topBar;
    private RecyclerView recyclerVideoGrid;
    private MaterialCardView selfPreviewCard;
    private MaterialCardView controlBar;
    private FrameLayout selfVideoContainer;
    private LinearLayout selfCameraOffPlaceholder;
    private ImageView btnFlipCamera;
    private ImageView btnMic, btnCamera, btnMore, btnEndCall;
    private ImageView btnRecord;
    private View btnMicContainer, btnCameraContainer, btnMoreContainer, btnEndCallContainer;
    private View btnRecordContainer;
    private View btnLayoutPicker;
    private View btnWaitingRoomContainer;
    private TextView tvWaitingRoomBadge;
    private TextView tvTimer, tvParticipantCount;
    private ImageView imgConnectionQuality;
    private LinearLayout recordingIndicator;
    private View recordingDot;

    // Self-view rendering
    private SurfaceViewRenderer selfSurfaceRenderer;
    private LocalVideoTrack currentLocalVideoTrack;

    // Auto-hide controls
    private Handler autoHideHandler;
    private Runnable autoHideRunnable;
    private boolean controlsVisible = true;

    // Recording indicator animation
    private android.animation.ObjectAnimator pulseAnimator;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_active_call, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupSelfPreview();
        setupListeners();
        setupObservers();
        setupAutoHide();

        // Start timer (idempotent - won't restart if already running)
        viewModel.startCallTimer();

        RoomConnectionState state = viewModel.getConnectionState().getValue();
        if (state == null
                || state == RoomConnectionState.DISCONNECTED
                || state == RoomConnectionState.FAILED) {
            viewModel.connectToRoom();
        }
    }

    private void initViews(View view) {
        rootLayout = view.findViewById(R.id.rootLayout);
        topBar = view.findViewById(R.id.topBar);
        recyclerVideoGrid = view.findViewById(R.id.recyclerVideoGrid);
        selfPreviewCard = view.findViewById(R.id.selfPreviewCard);
        controlBar = view.findViewById(R.id.controlBar);
        selfVideoContainer = view.findViewById(R.id.selfVideoContainer);
        selfCameraOffPlaceholder = view.findViewById(R.id.selfCameraOffPlaceholder);
        btnFlipCamera = view.findViewById(R.id.btnFlipCamera);
        btnMic = view.findViewById(R.id.btnMic);
        btnCamera = view.findViewById(R.id.btnCamera);
        btnMore = view.findViewById(R.id.btnMore);
        btnEndCall = view.findViewById(R.id.btnEndCall);
        btnMicContainer = view.findViewById(R.id.btnMicContainer);
        btnCameraContainer = view.findViewById(R.id.btnCameraContainer);
        btnMoreContainer = view.findViewById(R.id.btnMoreContainer);
        btnEndCallContainer = view.findViewById(R.id.btnEndCallContainer);
        btnRecord = view.findViewById(R.id.btnRecord);
        btnRecordContainer = view.findViewById(R.id.btnRecordContainer);
        btnLayoutPicker = view.findViewById(R.id.btnLayoutPicker);
        btnWaitingRoomContainer = view.findViewById(R.id.btnWaitingRoomContainer);
        tvWaitingRoomBadge = view.findViewById(R.id.tvWaitingRoomBadge);
        tvTimer = view.findViewById(R.id.tvTimer);
        tvParticipantCount = view.findViewById(R.id.tvParticipantCount);
        imgConnectionQuality = view.findViewById(R.id.imgConnectionQuality);
        recordingIndicator = view.findViewById(R.id.recordingIndicator);
        recordingDot = view.findViewById(R.id.recordingDot);
    }

    private void setupRecyclerView() {
        videoGridAdapter = new VideoGridAdapter();
        recyclerVideoGrid.setAdapter(videoGridAdapter);

        recyclerVideoGrid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSelfPreview() {
        selfSurfaceRenderer = new SurfaceViewRenderer(requireContext());
        selfSurfaceRenderer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        EglBase.Context eglContext = EglBase.create().getEglBaseContext();
        selfSurfaceRenderer.init(eglContext, null);
        selfSurfaceRenderer.setMirror(true);
        selfSurfaceRenderer.setEnableHardwareScaler(true);

        selfVideoContainer.addView(selfSurfaceRenderer);

        selfPreviewCard.setOnTouchListener(new SelfPreviewDragListener());
    }

    private void setupListeners() {
        // Mic toggle
        btnMicContainer.setOnClickListener(v -> {
            viewModel.toggleLocalMic();
            resetAutoHideTimer();
        });

        // Camera toggle
        btnCameraContainer.setOnClickListener(v -> {
            viewModel.toggleLocalCamera();
            resetAutoHideTimer();
        });

        // End call
        btnEndCallContainer.setOnClickListener(v -> showLeaveDialog());

        // More actions (overflow bottom sheet)
        btnMoreContainer.setOnClickListener(v -> {
            showMeetingActionsSheet();
            resetAutoHideTimer();
        });

        btnRecordContainer.setOnClickListener(v -> {
            viewModel.toggleRecording();
            resetAutoHideTimer();
        });

        btnLayoutPicker.setOnClickListener(v -> {
            showLayoutPicker();
            resetAutoHideTimer();
        });

        btnWaitingRoomContainer.setOnClickListener(v -> {
            showWaitingRoomSheet();
            resetAutoHideTimer();
        });

        btnFlipCamera.setOnClickListener(v -> {
            viewModel.switchCamera();
            resetAutoHideTimer();
        });

        // Tap to show/hide controls
        rootLayout.setOnClickListener(v -> toggleControlsVisibility());
    }

    private void showMeetingActionsSheet() {
        MeetingActionsBottomSheet sheet = new MeetingActionsBottomSheet();
        sheet.setCallback(this);
        sheet.show(getChildFragmentManager(), MeetingActionsBottomSheet.TAG);
    }

    private void showLayoutPicker() {
        LayoutPickerBottomSheet sheet = new LayoutPickerBottomSheet();
        sheet.show(getChildFragmentManager(), LayoutPickerBottomSheet.TAG);
    }

    private void showWaitingRoomSheet() {
        WaitingRoomBottomSheet sheet = new WaitingRoomBottomSheet();
        sheet.show(getChildFragmentManager(), WaitingRoomBottomSheet.TAG);
    }

    @Override
    public void onScreenShareClicked() {
        Snackbar.make(requireView(), R.string.feature_coming_soon, Snackbar.LENGTH_SHORT)
                .show();
    }

    @Override
    public void onChatClicked() {
        String meetingId = viewModel.getMeetingId().getValue();
        MeetingChatBottomSheet chatSheet = MeetingChatBottomSheet.newInstance(meetingId);
        chatSheet.show(getChildFragmentManager(), MeetingChatBottomSheet.TAG);
        resetAutoHideTimer();
    }

    @Override
    public void onParticipantsClicked() {
        ParticipantsBottomSheet participantsSheet = new ParticipantsBottomSheet();
        participantsSheet.show(getChildFragmentManager(), ParticipantsBottomSheet.TAG);
        resetAutoHideTimer();
    }

    @Override
    public void onChangeLayoutClicked() {
        showLayoutPicker();
    }

    @Override
    public void onMeetingSettingsClicked() {
        MeetingSettingsBottomSheet settingsSheet = new MeetingSettingsBottomSheet();
        settingsSheet.show(getChildFragmentManager(), MeetingSettingsBottomSheet.TAG);
        resetAutoHideTimer();
    }

    private void setupObservers() {
        viewModel.getCallDuration().observe(getViewLifecycleOwner(), seconds -> {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            tvTimer.setText(String.format(Locale.ROOT, "%02d:%02d", minutes, secs));
        });

        viewModel.isMicEnabled().observe(getViewLifecycleOwner(), enabled -> {
            updateMicButton(enabled);
        });

        viewModel.isCameraEnabled().observe(getViewLifecycleOwner(), enabled -> {
            updateCameraButton(enabled);
            updateSelfPreview(enabled);
        });

        viewModel
                .getConnectionState()
                .observe(getViewLifecycleOwner(), this::updateConnectionIndicator);

        viewModel.getParticipants().observe(getViewLifecycleOwner(), this::updateParticipants);

        viewModel.getActiveSpeakers().observe(getViewLifecycleOwner(), speakerIds -> {
            videoGridAdapter.setActiveSpeakers(new HashSet<>(speakerIds));
        });

        viewModel
                .getLocalVideoTrack()
                .observe(getViewLifecycleOwner(), this::attachLocalVideoTrack);

        viewModel.getCurrentLayout().observe(getViewLifecycleOwner(), this::applyLayout);

        viewModel.isHost().observe(getViewLifecycleOwner(), isHost -> {
            updateWaitingRoomButtonVisibility(
                    isHost, viewModel.getMeetingSettings().getValue());
            btnRecordContainer.setVisibility(isHost ? View.VISIBLE : View.GONE);
        });

        viewModel.getMeetingSettings().observe(getViewLifecycleOwner(), settings -> {
            Boolean isHost = viewModel.isHost().getValue();
            updateWaitingRoomButtonVisibility(isHost != null && isHost, settings);
        });

        viewModel.getPendingCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > 0) {
                tvWaitingRoomBadge.setText(String.valueOf(count));
                tvWaitingRoomBadge.setVisibility(View.VISIBLE);
            } else {
                tvWaitingRoomBadge.setVisibility(View.GONE);
            }
        });

        viewModel.isRecording().observe(getViewLifecycleOwner(), this::updateRecordingState);

        viewModel
                .isRecordingLoading()
                .observe(getViewLifecycleOwner(), this::updateRecordingLoading);

        viewModel.getRecordingError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
                viewModel.clearRecordingError();
            }
        });
    }

    private void setupAutoHide() {
        autoHideHandler = new Handler(Looper.getMainLooper());
        autoHideRunnable = () -> setControlsVisible(false);
        resetAutoHideTimer();
    }

    private void resetAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
        if (controlsVisible) {
            autoHideHandler.postDelayed(autoHideRunnable, CONTROLS_AUTO_HIDE_DELAY_MS);
        }
    }

    private void toggleControlsVisibility() {
        setControlsVisible(!controlsVisible);
    }

    private void setControlsVisible(boolean visible) {
        controlsVisible = visible;
        float alpha = visible ? 1f : 0f;
        int duration = 200;

        topBar.animate().alpha(alpha).setDuration(duration).start();
        controlBar.animate().alpha(alpha).setDuration(duration).start();
        selfPreviewCard.animate().alpha(alpha).setDuration(duration).start();

        if (visible) {
            resetAutoHideTimer();
        }
    }

    private void updateMicButton(boolean enabled) {
        if (enabled) {
            btnMic.setImageResource(R.drawable.ic_mic);
            btnMic.setBackgroundResource(R.drawable.bg_circle_blue);
            btnMic.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.white, null)));
            btnMic.setContentDescription(getString(R.string.cd_mute_mic));
        } else {
            btnMic.setImageResource(R.drawable.ic_mic_off);
            btnMic.setBackgroundResource(R.drawable.bg_control_button);
            btnMic.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.video_call_text_primary, null)));
            btnMic.setContentDescription(getString(R.string.cd_unmute_mic));
        }
    }

    private void updateCameraButton(boolean enabled) {
        if (enabled) {
            btnCamera.setImageResource(R.drawable.ic_videocam);
            btnCamera.setBackgroundResource(R.drawable.bg_circle_blue);
            btnCamera.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.white, null)));
            btnCamera.setContentDescription(getString(R.string.cd_disable_camera));
        } else {
            btnCamera.setImageResource(R.drawable.ic_videocam_off);
            btnCamera.setBackgroundResource(R.drawable.bg_control_button);
            btnCamera.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.video_call_text_primary, null)));
            btnCamera.setContentDescription(getString(R.string.cd_enable_camera));
        }
    }

    private void updateSelfPreview(boolean cameraEnabled) {
        if (cameraEnabled) {
            selfCameraOffPlaceholder.setVisibility(View.GONE);
            selfSurfaceRenderer.setVisibility(View.VISIBLE);
            btnFlipCamera.setVisibility(View.VISIBLE);
        } else {
            selfCameraOffPlaceholder.setVisibility(View.VISIBLE);
            selfSurfaceRenderer.setVisibility(View.GONE);
            btnFlipCamera.setVisibility(View.GONE);
        }
    }

    private void attachLocalVideoTrack(LocalVideoTrack track) {
        if (track == currentLocalVideoTrack) {
            return;
        }

        if (currentLocalVideoTrack != null) {
            currentLocalVideoTrack.removeRenderer(selfSurfaceRenderer);
        }

        currentLocalVideoTrack = track;

        if (track != null) {
            track.addRenderer(selfSurfaceRenderer);
        }
    }

    private void updateWaitingRoomButtonVisibility(
            boolean isHost, io.github.phunguy65.zms.domain.model.MeetingSettings settings) {
        boolean visible = isHost && settings != null && settings.isWaitingRoomEnabled();
        btnWaitingRoomContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void updateRecordingState(boolean isRecording) {
        recordingIndicator.setVisibility(isRecording ? View.VISIBLE : View.GONE);

        if (isRecording) {
            startPulseAnimation();
        } else {
            stopPulseAnimation();
        }

        if (isRecording) {
            btnRecord.setImageResource(R.drawable.ic_record);
            btnRecord.setBackgroundResource(R.drawable.bg_end_call_button);
            btnRecord.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.white, null)));
            btnRecord.setContentDescription(getString(R.string.cd_stop_recording));
        } else {
            btnRecord.setImageResource(R.drawable.ic_record);
            btnRecord.setBackgroundResource(R.drawable.bg_control_button);
            btnRecord.setImageTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.video_call_text_primary, null)));
            btnRecord.setContentDescription(getString(R.string.cd_start_recording));
        }
    }

    private void updateRecordingLoading(boolean isLoading) {
        btnRecordContainer.setEnabled(!isLoading);
        btnRecord.setAlpha(isLoading ? 0.5f : 1.0f);
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            return;
        }
        pulseAnimator = android.animation.ObjectAnimator.ofFloat(recordingDot, "alpha", 1f, 0.3f);
        pulseAnimator.setDuration(800);
        pulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        recordingDot.setAlpha(1f);
    }

    private void updateConnectionIndicator(RoomConnectionState state) {
        int color;
        switch (state) {
            case CONNECTED:
                color = R.color.video_call_active_speaker;
                break;
            case RECONNECTING:
                color = R.color.md_theme_light_error;
                break;
            case CONNECTING:
                color = R.color.md_theme_light_primary;
                break;
            default:
                color = R.color.video_call_text_secondary;
                break;
        }
        imgConnectionQuality.setImageTintList(
                android.content.res.ColorStateList.valueOf(getResources().getColor(color, null)));
    }

    private void updateParticipants(List<VideoParticipant> participants) {
        videoGridAdapter.submitList(participants);

        int count = participants.size() + 1;
        tvParticipantCount.setText(String.valueOf(count));

        VideoLayout currentLayout = viewModel.getCurrentLayout().getValue();
        int spanCount = calculateSpanCount(participants.size(), currentLayout);
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerVideoGrid.getLayoutManager();
        if (layoutManager != null && layoutManager.getSpanCount() != spanCount) {
            layoutManager.setSpanCount(spanCount);
        }
    }

    private void applyLayout(VideoLayout layout) {
        List<VideoParticipant> participants = viewModel.getParticipants().getValue();
        int participantCount = participants != null ? participants.size() : 0;
        int spanCount = calculateSpanCount(participantCount, layout);
        GridLayoutManager layoutManager = (GridLayoutManager) recyclerVideoGrid.getLayoutManager();
        if (layoutManager != null && layoutManager.getSpanCount() != spanCount) {
            layoutManager.setSpanCount(spanCount);
        }
    }

    private int calculateSpanCount(int participantCount, VideoLayout layout) {
        if (layout == null) {
            layout = VideoLayout.AUTO;
        }

        return switch (layout) {
            case AUTO -> {
                if (participantCount <= 1) {
                    yield 1;
                } else if (participantCount <= 4) {
                    yield 2;
                } else {
                    yield 3;
                }
            }
            case TILED -> 2;
            case SPOTLIGHT -> 1;
            case SIDEBAR -> 2;
        };
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.call_leave_title)
                .setMessage(R.string.call_leave_message)
                .setPositiveButton(R.string.call_leave_confirm, (dialog, which) -> {
                    viewModel.endCall();
                    requireActivity().finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoHideHandler.removeCallbacks(autoHideRunnable);
        stopPulseAnimation();

        if (currentLocalVideoTrack != null && selfSurfaceRenderer != null) {
            currentLocalVideoTrack.removeRenderer(selfSurfaceRenderer);
        }
        if (selfSurfaceRenderer != null) {
            selfVideoContainer.removeView(selfSurfaceRenderer);
            selfSurfaceRenderer.release();
            selfSurfaceRenderer = null;
        }
    }

    /**
     * Touch listener for making the self-preview draggable.
     */
    private class SelfPreviewDragListener implements View.OnTouchListener {
        private float dX, dY;
        private int lastAction;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    lastAction = MotionEvent.ACTION_DOWN;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;

                    View parent = (View) v.getParent();
                    newX = Math.max(0, Math.min(newX, parent.getWidth() - v.getWidth()));
                    newY = Math.max(0, Math.min(newY, parent.getHeight() - v.getHeight()));

                    v.setX(newX);
                    v.setY(newY);
                    lastAction = MotionEvent.ACTION_MOVE;
                    return true;

                case MotionEvent.ACTION_UP:
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        toggleControlsVisibility();
                    }
                    return true;

                default:
                    return false;
            }
        }
    }
}
