package io.github.phunguy65.zms.presentation.meeting.chat;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

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
