package io.github.phunguy65.zms.presentation.videocall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.color.MaterialColors;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import java.util.Locale;

/**
 * Active video call fragment.
 * Displays video grid, call controls, and provides access to participants and chat.
 * Converted from MeetingRoomActivity to work within VideoCallActivity.
 */
@AndroidEntryPoint
public class ActiveCallFragment extends Fragment {

    private CallViewModel viewModel;

    // Views
    private TextView btnLeave, tvTimer;
    private ImageView btnFloatVideo, btnFloatMic, btnFloatChat;
    private View btnNAVParticipant,
            btnFloatVideoContainer,
            btnFloatMicContainer,
            btnFloatChatContainer;

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
        setupListeners();
        setupObservers();

        // Start timer via ViewModel (survives config changes)
        viewModel.startCallTimer();
    }

    private void initViews(View view) {
        btnLeave = view.findViewById(R.id.btnLeave);
        tvTimer = view.findViewById(R.id.tvTimer);
        btnFloatVideo = view.findViewById(R.id.btnFloatVideo);
        btnFloatMic = view.findViewById(R.id.btnFloatMic);
        btnFloatChat = view.findViewById(R.id.btnFloatChat);
        btnNAVParticipant = view.findViewById(R.id.btnParticipantsContainer);
        btnFloatVideoContainer = view.findViewById(R.id.btnFloatVideoContainer);
        btnFloatMicContainer = view.findViewById(R.id.btnFloatMicContainer);
        btnFloatChatContainer = view.findViewById(R.id.btnFloatChatContainer);
    }

    private void setupListeners() {
        // Leave button
        btnLeave.setOnClickListener(v -> showLeaveDialog());

        // Camera toggle
        btnFloatVideoContainer.setOnClickListener(v -> viewModel.toggleCamera());

        // Mic toggle
        btnFloatMicContainer.setOnClickListener(v -> viewModel.toggleMic());

        // Chat button
        btnFloatChatContainer.setOnClickListener(v -> {
            MeetingChatBottomSheet chatSheet = new MeetingChatBottomSheet();
            chatSheet.show(getChildFragmentManager(), MeetingChatBottomSheet.TAG);
        });

        // Participants button
        btnNAVParticipant.setOnClickListener(v -> {
            ParticipantsBottomSheet participantsSheet = new ParticipantsBottomSheet();
            participantsSheet.show(getChildFragmentManager(), ParticipantsBottomSheet.TAG);
        });
    }

    private void setupObservers() {
        // Call duration - observe ViewModel's timer
        viewModel.getCallDuration().observe(getViewLifecycleOwner(), seconds -> {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            tvTimer.setText(String.format(Locale.ROOT, "%02d:%02d", minutes, secs));
        });

        // Mic state
        viewModel.isMicEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (enabled) {
                btnFloatMic.setBackgroundResource(R.drawable.bg_circle_blue);
                btnFloatMic.setImageTintList(
                        android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                                requireView(), com.google.android.material.R.attr.colorOnPrimary)));
                btnFloatMic.setContentDescription(getString(R.string.cd_mute_mic));
            } else {
                btnFloatMic.setBackgroundResource(R.drawable.bg_circle_white);
                btnFloatMic.setImageTintList(
                        android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                                requireView(), com.google.android.material.R.attr.colorOnSurface)));
                btnFloatMic.setContentDescription(getString(R.string.cd_unmute_mic));
            }
        });

        // Camera state
        viewModel.isCameraEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (enabled) {
                btnFloatVideo.setBackgroundResource(R.drawable.bg_circle_blue);
                btnFloatVideo.setImageTintList(
                        android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                                requireView(), com.google.android.material.R.attr.colorOnPrimary)));
                btnFloatVideo.setContentDescription(getString(R.string.cd_disable_camera));
            } else {
                btnFloatVideo.setBackgroundResource(R.drawable.bg_circle_white);
                btnFloatVideo.setImageTintList(
                        android.content.res.ColorStateList.valueOf(MaterialColors.getColor(
                                requireView(), com.google.android.material.R.attr.colorOnSurface)));
                btnFloatVideo.setContentDescription(getString(R.string.cd_enable_camera));
            }
        });
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
}
