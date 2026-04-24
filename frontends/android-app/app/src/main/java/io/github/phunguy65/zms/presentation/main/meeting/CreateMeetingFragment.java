package io.github.phunguy65.zms.presentation.main.meeting;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.meeting.create.CreateMeetingViewModel;
import io.github.phunguy65.zms.presentation.videocall.VideoCallActivity;

/**
 * Fragment for creating a new meeting.
 * Converted from CreateMeetingActivity to support single-activity navigation.
 *
 * <p>Initializes mic/camera toggle states from persisted preferences.
 */
@AndroidEntryPoint
public class CreateMeetingFragment extends Fragment {

    private CreateMeetingViewModel viewModel;

    private ImageView btnBack;
    private MaterialSwitch switchVideo, switchAudio;
    private MaterialButton btnStartMeeting, btnCopyLink;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_meeting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CreateMeetingViewModel.class);

        initViews(view);
        initFromSavedState();
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        switchVideo = view.findViewById(R.id.switchVideo);
        switchAudio = view.findViewById(R.id.switchAudio);
        btnStartMeeting = view.findViewById(R.id.btnStartMeeting);
        btnCopyLink = view.findViewById(R.id.btnCopyLink);
    }

    /**
     * Initializes mic/camera switches from saved preferences via ViewModel.
     */
    private void initFromSavedState() {
        boolean savedMicEnabled = viewModel.getLastMicEnabled();
        boolean savedCameraEnabled = viewModel.getLastCameraEnabled();

        switchAudio.setChecked(savedMicEnabled);
        switchVideo.setChecked(savedCameraEnabled);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        btnCopyLink.setOnClickListener(v -> {
            String meetingLink = viewModel.getMeetingLink();
            if (meetingLink != null) {
                copyToClipboard(meetingLink);
                Snackbar.make(v, R.string.create_meeting_link_copied, Snackbar.LENGTH_SHORT)
                        .show();
            } else {
                Snackbar.make(v, R.string.create_meeting_starting, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        btnStartMeeting.setOnClickListener(v -> {
            boolean isVideoOn = switchVideo.isChecked();
            boolean isAudioOn = switchAudio.isChecked();

            viewModel.startNewMeeting(isVideoOn, isAudioOn);
        });
    }

    private void setupObservers() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            btnStartMeeting.setEnabled(!isLoading);
            btnCopyLink.setEnabled(!isLoading);
            if (isLoading) {
                btnStartMeeting.setText(R.string.meeting_creation_loading);
            } else {
                btnStartMeeting.setText(R.string.create_meeting_start);
            }
        });

        viewModel.meetingSuccess.observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getShortCode() != null) {
                launchVideoCall(result.getShortCode(), result.getMeetingId());
            }
        });

        viewModel.meetingError.observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.meetingResult.observe(getViewLifecycleOwner(), result -> {
            btnCopyLink.setEnabled(result != null && result.getShortCode() != null);
        });
    }

    /**
     * Launches VideoCallActivity with meeting short code and UUID.
     *
     * @param meetingCode the short code for joining (used by JoinRoomRepository)
     * @param meetingId   the UUID for API calls (getMeetingDetail, updateMeetingSettings)
     */
    private void launchVideoCall(String meetingCode, String meetingId) {
        Intent intent = new Intent(requireContext(), VideoCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(VideoCallActivity.EXTRA_MEETING_CODE, meetingCode);
        intent.putExtra(VideoCallActivity.EXTRA_IS_GUEST, false);
        if (meetingId != null) {
            intent.putExtra(VideoCallActivity.EXTRA_MEETING_ID, meetingId);
        }
        startActivity(intent);
    }

    /**
     * Copies the given text to the system clipboard.
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard =
                (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Meeting Code", text);
            clipboard.setPrimaryClip(clip);
        }
    }
}
