package com.example.zeromeeting.view.meetingroom;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class ParticipantsActivity extends AppCompatActivity {

    private ParticipantsViewModel viewModel;
    private ParticipantAdapter adapter;

    private ImageView btnClose;
    private RecyclerView rvParticipants;
    private MaterialButton btnMuteAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants);

        viewModel = new ViewModelProvider(this).get(ParticipantsViewModel.class);

        initViews();
        setupRecyclerView();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        rvParticipants = findViewById(R.id.rvParticipants);
        btnMuteAll = findViewById(R.id.btnMuteAll);
    }

    private void setupRecyclerView() {
        rvParticipants.setLayoutManager(new LinearLayoutManager(this));
        // Để tránh lỗi nếu dữ liệu chưa có, ta set adapter null hoặc rỗng trước
        // Adapter sẽ được gán dữ liệu thực tế trong hàm setupObservers
    }

    private void setupObservers() {
        // Đúng chuẩn MVVM: Activity chỉ quan sát dữ liệu thay đổi từ ViewModel để cập nhật UI
        viewModel.getParticipants().observe(this, participants -> {
            adapter = new ParticipantAdapter(participants);
            rvParticipants.setAdapter(adapter);
        });
    }

    private void setupListeners() {
        // Đóng màn hình
        btnClose.setOnClickListener(v -> finish());

        // Xử lý nút Mute All
        btnMuteAll.setOnClickListener(v -> {
            viewModel.muteAllParticipants();
            Toast.makeText(this, "Muted all participants", Toast.LENGTH_SHORT).show();
        });
    }
}
