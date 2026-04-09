package com.example.zeromeeting.view.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.zeromeeting.view.guest.JoinGuestActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class DashboardActivity extends AppCompatActivity {

    private DashboardViewModel viewModel;

    private MaterialCardView cardNewMeeting, cardJoinMeeting, cardSchedule;
    private BottomNavigationView bottomNavigation;
    private ImageView btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        cardNewMeeting = findViewById(R.id.cardNewMeeting);
        cardJoinMeeting = findViewById(R.id.cardJoinMeeting);
        cardSchedule = findViewById(R.id.cardSchedule);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void setupListeners() {
        // Sự kiện các nút chức năng chính
        cardNewMeeting.setOnClickListener(v -> {
            Toast.makeText(this, "Mở màn hình Tạo phòng họp nhanh", Toast.LENGTH_SHORT)
                    .show();
            // startActivity(new Intent(this, CreateMeetingActivity.class));
        });

        cardJoinMeeting.setOnClickListener(v -> {
            // Tạm thời gọi lại màn hình Join Meeting mà chúng ta đã làm
            startActivity(new Intent(this, JoinGuestActivity.class));
        });

        cardSchedule.setOnClickListener(v -> {
            Toast.makeText(this, "Mở tính năng Lên lịch họp", Toast.LENGTH_SHORT)
                    .show();
        });

        btnSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Mở Cài đặt", Toast.LENGTH_SHORT).show();
        });

        // Xử lý sự kiện khi bấm vào các Tab ở thanh điều hướng dưới đáy
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_calendar) {
                Toast.makeText(this, "Chuyển sang tab Lịch", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Chuyển sang tab Cá nhân", Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
            return false;
        });
    }
}
