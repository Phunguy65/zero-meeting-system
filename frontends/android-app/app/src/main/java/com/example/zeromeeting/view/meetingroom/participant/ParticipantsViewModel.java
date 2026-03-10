package com.example.zeromeeting.view.meetingroom.participant;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class ParticipantsViewModel extends ViewModel {

    private final MutableLiveData<List<Participant>> participantsLiveData = new MutableLiveData<>();

    @Inject
    public ParticipantsViewModel() {
        loadMockData(); // Khởi tạo dữ liệu giả lập ngay khi mở ViewModel
    }

    public LiveData<List<Participant>> getParticipants() {
        return participantsLiveData;
    }

    private void loadMockData() {
        List<Participant> list = new ArrayList<>();
        // name, role, connection, isMicOn, isVideoOn, hasAlert
        list.add(new Participant("John Doe", "(Host, Me)", "", true, true, false));
        list.add(new Participant("Jane Smith", "", "", false, true, true)); // Bị tắt mic, có chấm đỏ
        list.add(new Participant("Alex Johnson", "", "", true, false, false));
        list.add(new Participant("Emily Davis", "", "Connecting...", false, false, false));

        participantsLiveData.setValue(list);
    }

    public void muteAllParticipants() {
        // Gọi ApiService thông qua Repository để tắt mic toàn bộ phòng
    }
}
