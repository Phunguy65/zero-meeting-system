package com.example.zeromeeting.view.meetingroom;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class JoinMeetingViewModel extends ViewModel {

    @Inject
    public JoinMeetingViewModel() {
        // Sau này inject MeetingRepository để xử lý API vào phòng
    }

    public void joinMeeting(String meetingId, boolean isAudioOn, boolean isVideoOn) {
        // Logic gửi request tham gia phòng họp lên server
    }
}
