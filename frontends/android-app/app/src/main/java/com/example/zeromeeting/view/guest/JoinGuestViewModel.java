package com.example.zeromeeting.view.guest; // Đổi lại package theo ý bạn

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class JoinGuestViewModel extends ViewModel {

    @Inject
    public JoinGuestViewModel() {
        // Có thể inject MeetingRepository vào đây sau
    }

    public void joinMeeting(
            String meetingId, String displayName, boolean isAudioOn, boolean isVideoOn) {
        // Xử lý logic validate ID và điều hướng vào phòng họp
    }
}
