package com.example.zeromeeting.view.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.example.zeromeeting.view.calendar.CalendarActivity;
import com.example.zeromeeting.view.dashboard.DashboardActivity;
import com.example.zeromeeting.view.welcome.WelcomeActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private ProfileViewModel viewModel;
    private BottomNavigationView bottomNavigation;

    private LinearLayout btnAccountSettings, btnMeetingHistory, btnHelpSupport;
    private MaterialCardView cardLogOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnAccountSettings = findViewById(R.id.btnAccountSettings);
        btnMeetingHistory = findViewById(R.id.btnMeetingHistory);
        btnHelpSupport = findViewById(R.id.btnHelpSupport);
        cardLogOut = findViewById(R.id.cardLogOut);

        // Đánh dấu Tab Profile là đang được chọn
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
    }

    private void setupListeners() {
        // Xử lý các nút Menu
        btnAccountSettings.setOnClickListener(v ->
                Toast.makeText(this, "Mở Cài đặt tài khoản", Toast.LENGTH_SHORT).show());
        btnMeetingHistory.setOnClickListener(v ->
                Toast.makeText(this, "Mở Lịch sử cuộc họp", Toast.LENGTH_SHORT).show());
        btnHelpSupport.setOnClickListener(
                v -> Toast.makeText(this, "Mở Trợ giúp", Toast.LENGTH_SHORT).show());

        // Xử lý nút Đăng xuất
        cardLogOut.setOnClickListener(v -> {
            viewModel.logOut();
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
            // Quay về màn hình Login và xóa toàn bộ Activity cũ (để user ko bấm Back quay lại được)
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Xử lý thanh Điều hướng đáy (Bottom Navigation)
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {
                startActivity(new Intent(this, CalendarActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
