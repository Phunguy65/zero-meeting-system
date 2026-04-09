package com.example.zeromeeting.view.meetingcreate;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.zeromeeting.view.dashboard.DashboardActivity;
import com.example.zeromeeting.view.meetingroom.MeetingRoomActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class CreateMeetingActivity extends AppCompatActivity {

    private CreateMeetingViewModel viewModel;

    private ImageView btnBack;
    private MaterialSwitch switchVideo, switchAudio;
    private MaterialButton btnStartMeeting, btnCopyLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_meeting);

        viewModel = new ViewModelProvider(this).get(CreateMeetingViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        switchVideo = findViewById(R.id.switchVideo);
        switchAudio = findViewById(R.id.switchAudio);
        btnStartMeeting = findViewById(R.id.btnStartMeeting);
        btnCopyLink = findViewById(R.id.btnCopyLink);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            Toast.makeText(this, "Chuyển sang tab Lịch", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CreateMeetingActivity.this, DashboardActivity.class));
            finish();
        });

        btnCopyLink.setOnClickListener(v -> {
            // Logic copy vào Clipboard
            Toast.makeText(this, "Meeting link copied to clipboard", Toast.LENGTH_SHORT)
                    .show();
        });

        btnStartMeeting.setOnClickListener(v -> {
            boolean isVideoOn = switchVideo.isChecked();
            boolean isAudioOn = switchAudio.isChecked();

            viewModel.startNewMeeting(isVideoOn, isAudioOn);
            Toast.makeText(this, "Starting meeting...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CreateMeetingActivity.this, MeetingRoomActivity.class));
            finish();
        });
    }
}
