package com.example.zeromeeting.core.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.zeromeeting.core.model.auth.LoginRequest;
import com.example.zeromeeting.core.model.auth.LoginResponse;
import com.example.zeromeeting.core.model.auth.RegisterRequest;
import com.example.zeromeeting.core.model.auth.RegisterResponse;
import com.example.zeromeeting.core.network.ApiService;
import com.example.zeromeeting.core.network.JsendResponse;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@Singleton
public class AuthRepository {

    private final ApiService apiService;

    // Hilt sẽ tự động "bơm" ApiService (đã cấu hình ở NetworkModule) vào đây
    @Inject
    public AuthRepository(ApiService apiService) {
        this.apiService = apiService;
    }

    // =====================================
    // HÀM ĐĂNG NHẬP
    // =====================================
    public LiveData<Resource<LoginResponse>> login(String email, String password) {
        MutableLiveData<Resource<LoginResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null)); // Báo cho UI biết là đang tải

        LoginRequest request = new LoginRequest(email, password);

        apiService.login(request).enqueue(new Callback<JsendResponse<LoginResponse>>() {
            @Override
            public void onResponse(Call<JsendResponse<LoginResponse>> call, Response<JsendResponse<LoginResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Thành công
                    liveData.setValue(Resource.success(response.body().getData()));
                } else {
                    // Lỗi từ server (ví dụ: sai mật khẩu)
                    String errorMsg = (response.body() != null) ? response.body().getMessage() : "Login Failed";
                    liveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<JsendResponse<LoginResponse>> call, Throwable t) {
                // Lỗi mạng, mất mạng, server sập...
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });

        return liveData;
    }

    // =====================================
    // HÀM ĐĂNG KÝ
    // =====================================
    public LiveData<Resource<RegisterResponse>> register(String email, String password, String fullName, String username) {
        MutableLiveData<Resource<RegisterResponse>> liveData = new MutableLiveData<>();
        liveData.setValue(Resource.loading(null));

        RegisterRequest request = new RegisterRequest(email, password, fullName, username);

        apiService.register(request).enqueue(new Callback<JsendResponse<RegisterResponse>>() {
            @Override
            public void onResponse(Call<JsendResponse<RegisterResponse>> call, Response<JsendResponse<RegisterResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    liveData.setValue(Resource.success(response.body().getData()));
                } else {
                    String errorMsg = (response.body() != null) ? response.body().getMessage() : "Registration Failed";
                    liveData.setValue(Resource.error(errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<JsendResponse<RegisterResponse>> call, Throwable t) {
                liveData.setValue(Resource.error(t.getMessage(), null));
            }
        });

        return liveData;
    }
}
