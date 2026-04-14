package com.example.zeromeeting.core.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.zeromeeting.core.model.user.UserResponse;
import com.example.zeromeeting.core.network.ApiService;
import com.example.zeromeeting.core.network.JsendResponse;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class UserRepository {

    private final ApiService apiService;

    @Inject
    public UserRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<Resource<UserResponse>> getMe() {
        MutableLiveData<Resource<UserResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        apiService.getMe().enqueue(new Callback<JsendResponse<UserResponse>>() {
            @Override
            public void onResponse(Call<JsendResponse<UserResponse>> call, Response<JsendResponse<UserResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.setValue(Resource.success(response.body().getData()));
                } else {
                    String errorMsg = (response.body() != null) ? response.body().getMessage() : "Không thể tải thông tin user";
                    liveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<JsendResponse<UserResponse>> call, Throwable t) {
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });

        return liveData;
    }
}
