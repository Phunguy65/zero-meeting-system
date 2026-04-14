package com.example.zeromeeting.view.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.zeromeeting.core.model.meeting.Meeting;
import com.example.zeromeeting.core.model.meeting.ScheduleMeetingRequest;
import com.example.zeromeeting.core.repository.MeetingRepository;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ScheduleViewModel extends ViewModel {

    private final MeetingRepository meetingRepository;

    @Inject
    public ScheduleViewModel(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    // Giao diện (Activity/Fragment) sẽ gọi hàm này khi người dùng điền xong form và bấm "Save/Schedule"
    public LiveData<Resource<Meeting>> scheduleNewMeeting(ScheduleMeetingRequest request) {
        return meetingRepository.scheduleMeeting(request);
    }
}
