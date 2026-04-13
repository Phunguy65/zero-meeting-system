package io.github.phunguy65.zms.presentation.meeting.create;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

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
