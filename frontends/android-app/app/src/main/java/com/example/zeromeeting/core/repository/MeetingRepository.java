package com.example.zeromeeting.core.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.zeromeeting.core.model.meeting.Meeting;
import com.example.zeromeeting.core.model.meeting.ScheduleMeetingRequest;
import com.example.zeromeeting.core.network.ApiService;
import com.example.zeromeeting.core.network.JsendResponse;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class MeetingRepository {

    private final ApiService apiService;

    @Inject
    public MeetingRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    // =====================================
    // LẤY THÔNG TIN CHI TIẾT PHÒNG HỌP
    // =====================================
    public LiveData<Resource<Meeting>> getMeetingDetail(String meetingId) {
        MutableLiveData<Resource<Meeting>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        apiService.getMeetingDetail(meetingId).enqueue(new Callback<JsendResponse<Meeting>>() {
            @Override
            public void onResponse(Call<JsendResponse<Meeting>> call, Response<JsendResponse<Meeting>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.setValue(Resource.success(response.body().getData()));
                } else {
                    String errorMsg = (response.body() != null) ? response.body().getMessage() : "Lỗi khi lấy thông tin phòng họp";
                    liveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<JsendResponse<Meeting>> call, Throwable t) {
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });

        return liveData;
    }

    // =====================================
    // LÊN LỊCH CUỘC HỌP MỚI (SCHEDULE)
    // =====================================
    public LiveData<Resource<Meeting>> scheduleMeeting(ScheduleMeetingRequest request) {
        MutableLiveData<Resource<Meeting>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        apiService.scheduleMeeting(request).enqueue(new Callback<JsendResponse<Meeting>>() {
            @Override
            public void onResponse(Call<JsendResponse<Meeting>> call, Response<JsendResponse<Meeting>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.setValue(Resource.success(response.body().getData()));
                } else {
                    String errorMsg = (response.body() != null) ? response.body().getMessage() : "Không thể lên lịch cuộc họp";
                    liveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<JsendResponse<Meeting>> call, Throwable t) {
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });

        return liveData;
    }

    // (Sau này bạn có thể copy format này để viết nốt các hàm như createInstantMeeting, startMeeting...)
}
