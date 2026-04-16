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
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.meeting.join.JoinMeetingViewModel;

/**
 * Fragment for joining an existing meeting.
 * Converted from JoinMeetingActivity to support single-activity navigation.
 */
@AndroidEntryPoint
public class JoinMeetingFragment extends Fragment {

    private JoinMeetingViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtMeetingId;
    private MaterialButton btnJoin;
    private MaterialSwitch switchAudio, switchVideo;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_join_meeting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(JoinMeetingViewModel.class);

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        edtMeetingId = view.findViewById(R.id.edtMeetingId);
        btnJoin = view.findViewById(R.id.btnJoin);
        switchAudio = view.findViewById(R.id.switchAudio);
        switchVideo = view.findViewById(R.id.switchVideo);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        btnJoin.setOnClickListener(v -> {
            String meetingId = edtMeetingId.getText() != null
                    ? edtMeetingId.getText().toString().trim()
                    : "";
            boolean isAudioOn = switchAudio.isChecked();
            boolean isVideoOn = switchVideo.isChecked();

            if (meetingId.isEmpty()) {
                Snackbar.make(v, R.string.join_meeting_error_empty_id, Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            viewModel.joinMeeting(meetingId, isAudioOn, isVideoOn);
            Snackbar.make(
                            v,
                            getString(R.string.join_meeting_joining, meetingId),
                            Snackbar.LENGTH_SHORT)
                    .show();

            // TODO: Navigate to VideoCallActivity when it's implemented
            // For now, just go back to dashboard after a successful join request
        });
    }
}
