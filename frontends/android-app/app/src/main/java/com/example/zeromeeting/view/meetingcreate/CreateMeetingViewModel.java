package com.example.zeromeeting.view.meetingcreate;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CreateMeetingViewModel extends ViewModel {

    @Inject
    public CreateMeetingViewModel() {
        // Sau này inject Repository để gọi API tạo phòng họp mới lấy ID
    }

    public void startNewMeeting(boolean isVideoOn, boolean isAudioOn) {
        // Logic tạo phòng họp
    }
}
