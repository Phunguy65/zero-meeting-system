package com.example.zeromeeting.view.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.view.meetingcreate.CreateMeetingActivity;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint // Vẫn giữ nguyên Hilt
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private MaterialCardView cardNewMeeting;

    // Khác biệt 2: Nạp Layout ở hàm onCreateView thay vì onCreate
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    // Khác biệt 3: Ánh xạ View và viết logic ở hàm onViewCreated
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // LƯU Ý: Phải có chữ "view." đằng trước findViewById
        cardNewMeeting = view.findViewById(R.id.cardNewMeeting);

        setupListeners();
    }

    private void setupListeners() {
        cardNewMeeting.setOnClickListener(v -> {
            // Khác biệt 4: Thay chữ "this" bằng "requireActivity()" hoặc "requireContext()"
            startActivity(new Intent(requireActivity(), CreateMeetingActivity.class));
        });
    }
}
