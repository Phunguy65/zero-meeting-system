package io.github.phunguy65.zms.presentation.main.meeting;

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
            // TODO: Get actual meeting link from ViewModel when meeting creation API is integrated
            // For now, show a message that the feature is not yet available
            Snackbar.make(v, R.string.create_meeting_link_copied, Snackbar.LENGTH_SHORT)
                    .show();
        });

        btnStartMeeting.setOnClickListener(v -> {
            boolean isVideoOn = switchVideo.isChecked();
            boolean isAudioOn = switchAudio.isChecked();

            viewModel.startNewMeeting(isVideoOn, isAudioOn);
            Snackbar.make(v, R.string.create_meeting_starting, Snackbar.LENGTH_SHORT)
                    .show();

            // TODO: Navigate to VideoCallActivity when it's implemented
            // For now, just go back to dashboard
            Navigation.findNavController(v).popBackStack();
        });
    }
}
