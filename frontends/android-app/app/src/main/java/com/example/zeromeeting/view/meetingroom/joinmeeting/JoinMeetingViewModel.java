package com.example.zeromeeting.view.meetingroom.joinmeeting;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

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
