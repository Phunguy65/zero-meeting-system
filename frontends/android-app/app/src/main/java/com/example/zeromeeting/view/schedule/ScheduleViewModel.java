package com.example.zeromeeting.view.schedule; // Đổi lại package theo ý bạn

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScheduleViewModel extends ViewModel {

    @Inject
    public ScheduleViewModel() {
        // Sau này gọi API lên lịch họp
    }

    public void scheduleMeeting(String topic, String date, String time, String duration, boolean isWaitingRoom, boolean isHostVideoOn) {
        // Xử lý gửi dữ liệu lịch họp lên server
    }
}
