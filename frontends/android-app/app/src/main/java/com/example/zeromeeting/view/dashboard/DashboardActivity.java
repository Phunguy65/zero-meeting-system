package com.example.zeromeeting.view.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.view.auth.login.LoginActivity;
import com.example.zeromeeting.view.auth.register.RegisterActivity;
import com.example.zeromeeting.view.calendar.CalendarActivity;
import com.example.zeromeeting.view.meetingcreate.CreateMeetingActivity;
import com.example.zeromeeting.view.profile.ProfileActivity;
import com.example.zeromeeting.view.schedule.ScheduleActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

// Import màn hình JoinGuestActivity đã làm trước đó
import com.example.zeromeeting.view.guest.JoinGuestActivity;

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
            Toast.makeText(this, "Mở màn hình Tạo phòng họp nhanh", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, CreateMeetingActivity.class));
            finish();
        });

        cardJoinMeeting.setOnClickListener(v -> {
            Toast.makeText(this, "Mở màn hình Vào phòng họp nhanh", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, CreateMeetingActivity.class));
            finish();
        });

        cardSchedule.setOnClickListener(v -> {
            Toast.makeText(this, "Mở tính năng Lên lịch họp", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, ScheduleActivity.class));
            finish();
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
                startActivity(new Intent(DashboardActivity.this, CalendarActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Chuyển sang tab Cá nhân", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DashboardActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
