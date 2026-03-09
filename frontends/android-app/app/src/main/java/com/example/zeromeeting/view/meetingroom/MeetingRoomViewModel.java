package com.example.zeromeeting.view.meetingroom;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MeetingRoomViewModel extends ViewModel {

    @Inject
    public MeetingRoomViewModel() {
        // Sau này sẽ inject WebRTC repository để quản lý luồng video/audio
    }

    public void leaveMeeting() {
        // Logic ngắt kết nối socket, tắt camera, mic
    }
}
