package com.example.zeromeeting.view.calendar;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CalendarViewModel extends ViewModel {

    @Inject
    public CalendarViewModel() {
        // Sau này gọi Repository để lấy danh sách lịch theo ngày
    }
}
