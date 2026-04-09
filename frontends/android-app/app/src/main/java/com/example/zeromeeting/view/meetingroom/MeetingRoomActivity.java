package com.example.zeromeeting.view.meetingroom;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.zeromeeting.view.meetingroom.chat.MeetingChatActivity;
import com.example.zeromeeting.view.meetingroom.participant.ParticipantsActivity;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class MeetingRoomActivity extends AppCompatActivity {

    private MeetingRoomViewModel viewModel;

    private TextView btnLeave;
    private ImageView btnFloatVideo, btnFloatMic, btnFloatChat, btnNAVParticipant;

    private boolean isMicOn = true;
    private boolean isVideoOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_room);

        viewModel = new ViewModelProvider(this).get(MeetingRoomViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnLeave = findViewById(R.id.btnLeave);
        btnFloatVideo = findViewById(R.id.btnFloatVideo);
        btnFloatMic = findViewById(R.id.btnFloatMic);
        btnFloatChat = findViewById(R.id.btnFloatChat);
        btnNAVParticipant = findViewById(R.id.btnNAVParticipant);
    }

    private void setupListeners() {
        // Nút rời cuộc họp
        btnLeave.setOnClickListener(v -> showLeaveDialog());

        // Bật/Tắt Mic nổi
        btnFloatMic.setOnClickListener(v -> {
            isMicOn = !isMicOn;
            if (isMicOn) {
                btnFloatMic.setBackgroundTintList(getResources()
                        .getColorStateList(android.R.color.holo_blue_light)); // Giả lập màu xanh
                Toast.makeText(this, "Microphone On", Toast.LENGTH_SHORT).show();
            } else {
                btnFloatMic.setBackgroundTintList(
                        getResources().getColorStateList(android.R.color.darker_gray));
                Toast.makeText(this, "Microphone Off", Toast.LENGTH_SHORT).show();
            }
        });

        // Bật/Tắt Video nổi
        btnFloatVideo.setOnClickListener(v -> {
            isVideoOn = !isVideoOn;
            Toast.makeText(this, isVideoOn ? "Camera On" : "Camera Off", Toast.LENGTH_SHORT)
                    .show();
        });

        // Nút Chat
        btnFloatChat.setOnClickListener(v -> {
            Toast.makeText(this, "Mở khung Chat", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MeetingRoomActivity.this, MeetingChatActivity.class));
        });
        btnNAVParticipant.setOnClickListener(v -> {
            Toast.makeText(this, "Mở trang người tham gia", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MeetingRoomActivity.this, ParticipantsActivity.class));
        });
    }

    private void showLeaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Meeting")
                .setMessage("Are you sure you want to leave this meeting?")
                .setPositiveButton("Leave", (dialog, which) -> {
                    viewModel.leaveMeeting();
                    finish(); // Thoát khỏi phòng họp, quay lại màn hình trước đó
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
