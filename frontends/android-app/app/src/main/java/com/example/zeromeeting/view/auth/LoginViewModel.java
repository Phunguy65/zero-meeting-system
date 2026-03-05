package com.example.zeromeeting.view.auth;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    @Inject
    public LoginViewModel() {
        // Sau này sẽ inject AuthRepository vào đây để gọi API
    }

    public void loginUser(String email, String password) {
        // Thực hiện logic validate dữ liệu và gọi API Đăng nhập
    }
}
