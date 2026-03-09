package com.example.zeromeeting.view.meetingroom.chat;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class MeetingChatViewModel extends ViewModel {

    @Inject
    public MeetingChatViewModel() {
        // Sau này sẽ gọi API hoặc WebSocket để lấy/gửi tin nhắn real-time ở đây
    }

    public void sendMessage(String message) {
        // Xử lý logic gửi tin nhắn
    }
}
