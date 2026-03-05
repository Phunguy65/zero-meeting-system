package com.example.zeromeeting.view.guest; // Đổi lại package theo ý bạn

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class JoinGuestActivity extends AppCompatActivity {

    private JoinGuestViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtMeetingId, edtDisplayName;
    private MaterialSwitch switchAudio, switchVideo;
    private TextView tvAudioStatus, tvVideoStatus;
    private MaterialButton btnJoinMeeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_guest);

        viewModel = new ViewModelProvider(this).get(JoinGuestViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtMeetingId = findViewById(R.id.edtMeetingId);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        switchAudio = findViewById(R.id.switchAudio);
        switchVideo = findViewById(R.id.switchVideo);
        tvAudioStatus = findViewById(R.id.tvAudioStatus);
        tvVideoStatus = findViewById(R.id.tvVideoStatus);
        btnJoinMeeting = findViewById(R.id.btnJoinMeeting);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Lắng nghe thay đổi của Switch Audio để cập nhật text
        switchAudio.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvAudioStatus.setText("Join with microphone on");
            } else {
                tvAudioStatus.setText("Join with microphone off");
            }
        });

        // Lắng nghe thay đổi của Switch Video để cập nhật text
        switchVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                tvVideoStatus.setText("Join with camera on");
            } else {
                tvVideoStatus.setText("Join with camera off");
            }
        });

        btnJoinMeeting.setOnClickListener(v -> {
            String meetingId = edtMeetingId.getText().toString().trim();
            String displayName = edtDisplayName.getText().toString().trim();
            boolean isAudioOn = switchAudio.isChecked();
            boolean isVideoOn = switchVideo.isChecked();

            if (meetingId.isEmpty()) {
                Toast.makeText(this, "Please enter Meeting ID", Toast.LENGTH_SHORT).show();
                return;
            }
            if (displayName.isEmpty()) {
                displayName = "Guest";
            }

            viewModel.joinMeeting(meetingId, displayName, isAudioOn, isVideoOn);
            Toast.makeText(this, "Joining meeting...", Toast.LENGTH_SHORT).show();
            // Sau này sẽ có Intent chuyển sang MeetingRoomActivity
        });
    }
}
