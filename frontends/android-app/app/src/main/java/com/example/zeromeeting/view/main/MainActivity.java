package com.example.zeromeeting.view.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Nạp Adapter cho ViewPager
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // (Tùy chọn) Vô hiệu hóa tính năng vuốt tự do nếu muốn ép người dùng chỉ bấm nút.
        // viewPager.setUserInputEnabled(false);

        setupSync();
    }

    private void setupSync() {
        // 1. Khi bấm vào Menu dưới đáy -> Chuyển ViewPager đến trang tương ứng
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                if (viewPager.getCurrentItem() != 0) viewPager.setCurrentItem(0, true); // Chữ 'true' để tạo hiệu ứng trượt mượt mà
            } else if (itemId == R.id.nav_calendar) {
                viewPager.setCurrentItem(1, true);
            } else if (itemId == R.id.nav_profile) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });

        // 2. Khi vuốt ViewPager -> Cập nhật trạng thái sáng của Menu dưới đáy
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
                        break;
                    case 1:
                        bottomNavigation.getMenu().findItem(R.id.nav_calendar).setChecked(true);
                        break;
                    case 2:
                        bottomNavigation.getMenu().findItem(R.id.nav_profile).setChecked(true);
                        break;
                }
            }
        });
    }
}
