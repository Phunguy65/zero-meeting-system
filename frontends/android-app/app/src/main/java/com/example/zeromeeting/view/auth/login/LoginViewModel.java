package com.example.zeromeeting.view.auth.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.zeromeeting.core.model.auth.LoginResponse;
import com.example.zeromeeting.core.repository.AuthRepository;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // Hilt tự động bơm AuthRepository vào đây
    @Inject
    public LoginViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // Hàm này sẽ được LoginActivity gọi khi bấm nút Đăng nhập
    public LiveData<Resource<LoginResponse>> login(String email, String password) {
        return authRepository.login(email, password);
    }
}
