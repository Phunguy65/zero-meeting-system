package com.example.zeromeeting.view.dashboard;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    @Inject
    public DashboardViewModel() {
        // Sau này inject Repository để gọi API lấy danh sách Meeting
    }
}
