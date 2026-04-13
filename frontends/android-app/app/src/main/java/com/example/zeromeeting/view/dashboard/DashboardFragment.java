package com.example.zeromeeting.view.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.view.meetingcreate.CreateMeetingActivity;
import com.example.zeromeeting.view.meetingroom.joinmeeting.JoinMeetingActivity;
import com.example.zeromeeting.view.schedule.ScheduleActivity;

import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;

    // 1. Khai báo thêm 2 biến card cho Join và Schedule
    private MaterialCardView cardNewMeeting;
    private MaterialCardView cardJoinMeeting;
    private MaterialCardView cardSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // 2. Ánh xạ đủ 3 View từ XML
        cardNewMeeting = view.findViewById(R.id.cardNewMeeting);
        cardJoinMeeting = view.findViewById(R.id.cardJoinMeeting);
        cardSchedule = view.findViewById(R.id.cardSchedule);

        setupListeners();
    }

    private void setupListeners() {
        // Nút New Meeting -> Mở CreateMeetingActivity
        cardNewMeeting.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), CreateMeetingActivity.class));
        });

        // Nút Join Meeting -> Mở JoinMeetingActivity
        cardJoinMeeting.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), JoinMeetingActivity.class));
        });

        // Nút Schedule -> Mở ScheduleActivity
        cardSchedule.setOnClickListener(v -> {
            startActivity(new Intent(requireActivity(), ScheduleActivity.class));
        });
    }
}
