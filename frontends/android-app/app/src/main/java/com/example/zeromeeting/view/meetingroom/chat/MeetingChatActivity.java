package com.example.zeromeeting.view.meetingroom.chat;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class MeetingChatActivity extends AppCompatActivity {

    private MeetingChatViewModel viewModel;

    private ImageView btnClose, btnAttach, btnSend;
    private EditText edtMessage;
    private RecyclerView rvChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_chat);

        viewModel = new ViewModelProvider(this).get(MeetingChatViewModel.class);

        initViews();
        setupRecyclerView();
        setupListeners();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        btnAttach = findViewById(R.id.btnAttach);
        btnSend = findViewById(R.id.btnSend);
        edtMessage = findViewById(R.id.edtMessage);
        rvChat = findViewById(R.id.rvChat);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Hiển thị tin nhắn mới nhất ở dưới cùng
        rvChat.setLayoutManager(layoutManager);

        // Bạn sẽ cần tạo một ChatAdapter ở đây (tương tự như màn Participants)
        // để phân loại tin nhắn gửi đi (màu xanh) và nhận về (màu xám).
    }

    private void setupListeners() {
        // Đóng màn hình chat
        btnClose.setOnClickListener(v -> finish());

        // Mở tệp đính kèm
        btnAttach.setOnClickListener(v -> Toast.makeText(this, "Mở trình chọn tệp", Toast.LENGTH_SHORT).show());

        // Gửi tin nhắn
        btnSend.setOnClickListener(v -> {
            String message = edtMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                viewModel.sendMessage(message);
                edtMessage.setText(""); // Xóa trắng ô nhập
                Toast.makeText(this, "Đã gửi", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
