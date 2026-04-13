package com.example.zeromeeting.view.meetingroom.joinmeeting;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class JoinMeetingActivity extends AppCompatActivity {

    private JoinMeetingViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtMeetingId;
    private MaterialButton btnJoin;
    private MaterialSwitch switchAudio, switchVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_meeting);

        viewModel = new ViewModelProvider(this).get(JoinMeetingViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtMeetingId = findViewById(R.id.edtMeetingId);
        btnJoin = findViewById(R.id.btnJoin);
        switchAudio = findViewById(R.id.switchAudio);
        switchVideo = findViewById(R.id.switchVideo);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnJoin.setEnabled(false);

        btnJoin.setOnClickListener(v -> {
            String meetingId = edtMeetingId.getText().toString().trim();
            boolean isAudioOn = switchAudio.isChecked();
            boolean isVideoOn = switchVideo.isChecked();

            if (meetingId.isEmpty()) {
                Toast.makeText(this, "Please enter a valid Meeting ID", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.joinMeeting(meetingId, isAudioOn, isVideoOn);
            Toast.makeText(this, "Joining meeting " + meetingId + "...", Toast.LENGTH_SHORT).show();
            // Điều hướng sang màn hình Phòng họp (MeetingRoomActivity) sau khi API trả về thành công
        });

        edtMeetingId.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Nếu có chữ thì bật nút, đổi màu xanh đậm. Nếu xóa hết thì tắt nút.
                boolean hasText = s.toString().trim().length() > 0;
                btnJoin.setEnabled(hasText);
                btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    hasText ? android.graphics.Color.parseColor("#1877F2") : android.graphics.Color.parseColor("#81B4F8")
                ));
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
}
