package io.github.phunguy65.zms.presentation.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.dashboard.DashboardActivity;
import io.github.phunguy65.zms.presentation.profile.ProfileActivity;

@AndroidEntryPoint
public class CalendarActivity extends AppCompatActivity {

    private CalendarViewModel viewModel;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Đánh dấu Tab Calendar là đang được chọn
        bottomNavigation.setSelectedItemId(R.id.nav_calendar);

        setupListeners();
    }

    private void setupListeners() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, DashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_calendar) {

                return true;
            } else if (itemId == R.id.nav_profile) {
                Toast.makeText(this, "Chuyển sang tab Cá nhân", Toast.LENGTH_SHORT)
                        .show();
                startActivity(new Intent(CalendarActivity.this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }
}
