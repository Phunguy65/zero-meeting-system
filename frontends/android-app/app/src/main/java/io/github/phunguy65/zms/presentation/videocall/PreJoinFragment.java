package io.github.phunguy65.zms.presentation.videocall;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Pre-join screen for video calls.
 * Handles both guest and authenticated user flows.
 * Includes meeting code input, display name (guest only), and audio/video toggles.
 */
@AndroidEntryPoint
public class PreJoinFragment extends Fragment {

    private CallViewModel viewModel;
    private NavController navController;

    // Views
    private View btnBackContainer;
    private TextInputLayout tilMeetingCode, tilDisplayName;
    private TextInputEditText edtMeetingCode, edtDisplayName;
    private TextView lblDisplayName, tvAudioStatus, tvVideoStatus;
    private MaterialSwitch switchAudio, switchVideo;
    private MaterialButton btnJoinMeeting;

    // Permission launcher
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean audioGranted = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);

                if (Boolean.TRUE.equals(cameraGranted) && Boolean.TRUE.equals(audioGranted)) {
                    // Permissions granted, proceed
                    proceedToCall();
                } else {
                    // Show rationale
                    Snackbar.make(requireView(),
                            R.string.permission_camera_mic_required,
                            Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get ViewModel scoped to VideoCallActivity
        viewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_prejoin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        initViews(view);
        setupGuestMode();
        setupListeners();
        setupObservers();
        prefillFromIntent();
    }

    private void initViews(View view) {
        btnBackContainer = view.findViewById(R.id.btnBackContainer);
        tilMeetingCode = view.findViewById(R.id.tilMeetingCode);
        tilDisplayName = view.findViewById(R.id.tilDisplayName);
        edtMeetingCode = view.findViewById(R.id.edtMeetingCode);
        edtDisplayName = view.findViewById(R.id.edtDisplayName);
        lblDisplayName = view.findViewById(R.id.lblDisplayName);
        tvAudioStatus = view.findViewById(R.id.tvAudioStatus);
        tvVideoStatus = view.findViewById(R.id.tvVideoStatus);
        switchAudio = view.findViewById(R.id.switchAudio);
        switchVideo = view.findViewById(R.id.switchVideo);
        btnJoinMeeting = view.findViewById(R.id.btnJoinMeeting);
    }

    private void setupGuestMode() {
        // Read isGuest from ViewModel (set by VideoCallActivity from intent extras)
        // This avoids tight coupling to the Activity class
        Boolean isGuest = viewModel.isGuest().getValue();
        boolean guestMode = Boolean.TRUE.equals(isGuest);

        // Show display name field for guests only
        int visibility = guestMode ? View.VISIBLE : View.GONE;
        lblDisplayName.setVisibility(visibility);
        tilDisplayName.setVisibility(visibility);
    }

    private void setupListeners() {
        // Back button
        btnBackContainer.setOnClickListener(v -> requireActivity().finish());

        // Audio switch
        switchAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setMicEnabled(isChecked);
            tvAudioStatus.setText(isChecked ? R.string.prejoin_audio_on : R.string.prejoin_audio_off);
        });

        // Video switch
        switchVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.setCameraEnabled(isChecked);
            tvVideoStatus.setText(isChecked ? R.string.prejoin_video_on : R.string.prejoin_video_off);
        });

        // Join button
        btnJoinMeeting.setOnClickListener(v -> onJoinClicked());
    }

    private void setupObservers() {
        // Sync switch states with ViewModel
        viewModel.isMicEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (switchAudio.isChecked() != enabled) {
                switchAudio.setChecked(enabled);
            }
        });

        viewModel.isCameraEnabled().observe(getViewLifecycleOwner(), enabled -> {
            if (switchVideo.isChecked() != enabled) {
                switchVideo.setChecked(enabled);
            }
        });
    }

    private void prefillFromIntent() {
        // Read meeting code from ViewModel (set by VideoCallActivity from intent extras)
        // This avoids tight coupling to the Activity class
        String meetingCode = viewModel.getMeetingCode().getValue();
        if (meetingCode != null && !meetingCode.isEmpty()) {
            edtMeetingCode.setText(meetingCode);
        }
    }

    private void onJoinClicked() {
        // Clear previous errors
        tilMeetingCode.setError(null);
        tilDisplayName.setError(null);

        // Get input values
        String meetingCode = edtMeetingCode.getText() != null
                ? edtMeetingCode.getText().toString().trim()
                : "";
        String displayName = edtDisplayName.getText() != null
                ? edtDisplayName.getText().toString().trim()
                : "";

        // Update ViewModel
        viewModel.setMeetingCode(meetingCode);
        viewModel.setDisplayName(displayName);

        // Validate
        boolean hasError = false;

        if (meetingCode.isEmpty()) {
            tilMeetingCode.setError(getString(R.string.prejoin_error_meeting_code));
            hasError = true;
        }

        Boolean isGuest = viewModel.isGuest().getValue();
        if (Boolean.TRUE.equals(isGuest) && displayName.isEmpty()) {
            tilDisplayName.setError(getString(R.string.prejoin_error_display_name));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        // Check permissions before proceeding
        requestPermissionsAndJoin();
    }

    private void requestPermissionsAndJoin() {
        boolean hasCameraPermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasAudioPermission = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;

        if (hasCameraPermission && hasAudioPermission) {
            proceedToCall();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            });
        }
    }

    private void proceedToCall() {
        navController.navigate(R.id.action_prejoin_to_activeCall);
    }
}
