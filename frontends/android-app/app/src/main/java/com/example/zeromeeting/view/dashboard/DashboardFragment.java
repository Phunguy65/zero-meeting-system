package com.example.zeromeeting.view.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.view.meetingcreate.CreateMeetingActivity;
import com.example.zeromeeting.view.meetingroom.joinmeeting.JoinMeetingActivity;
import com.example.zeromeeting.view.schedule.ScheduleActivity;
import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R; // Nhớ đổi đúng package R của bạn

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private TextView tvUserName;
    private MaterialCardView cardNewMeeting, cardJoinMeeting, cardSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Thay R.layout.fragment_dashboard bằng tên file XML mà bạn vừa gửi
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // 2. Ánh xạ View
        initViews(view);

        // 3. Cài đặt sự kiện bấm nút
        setupListeners();

        // 4. Lấy tên người dùng thay cho chữ "Meet AI"
        fetchUserName();
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        cardNewMeeting = view.findViewById(R.id.cardNewMeeting);
        cardJoinMeeting = view.findViewById(R.id.cardJoinMeeting);
        cardSchedule = view.findViewById(R.id.cardSchedule);
    }

    private void setupListeners() {
        // Nút New Meeting (Tạo phòng họp tức thì)
        cardNewMeeting.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), CreateMeetingActivity.class));
        });

        // Nút Join Meeting (Vào phòng bằng mã)
        cardJoinMeeting.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), JoinMeetingActivity.class));
        });

        // Nút Schedule (Lên lịch họp)
        cardSchedule.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ScheduleActivity.class));
        });
    }

    private void fetchUserName() {
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case SUCCESS:
                    if (resource.data != null) {
                        tvUserName.setText(resource.data.getFullName());
                    }
                    break;
                case ERROR:
                    // Nếu lỗi (ví dụ rớt mạng), cứ để nguyên chữ "Meet AI" hoặc báo lỗi nhẹ
                    Toast.makeText(getContext(), "Không thể tải tên người dùng", Toast.LENGTH_SHORT).show();
                    break;
                case LOADING:
                    tvUserName.setText("Đang tải...");
                    break;
            }
        });
    }
}
