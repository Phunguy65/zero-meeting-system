package com.example.zeromeeting.view.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.zeromeeting.view.calendar.CalendarFragment;
import com.example.zeromeeting.view.dashboard.DashboardFragment;
import com.example.zeromeeting.view.profile.ProfileFragment;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Sắp xếp thứ tự các màn hình tương ứng với thứ tự vuốt từ trái sang phải
        switch (position) {
            case 0:
                return new DashboardFragment();
            case 1:
                return new CalendarFragment();
            case 2:
                return new ProfileFragment();
            default:
                return new DashboardFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Tổng cộng chúng ta có 3 tab
    }
}
