package io.github.phunguy65.zms.presentation.calendar;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CalendarViewModel extends ViewModel {

    @Inject
    public CalendarViewModel() {
        // Sau này gọi Repository để lấy danh sách lịch theo ngày
    }
}
