package com.example.zeromeeting.view.auth;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RegisterViewModel extends ViewModel {

    @Inject
    public RegisterViewModel() {
        // Sau này sẽ inject AuthRepository vào đây
    }

    // Hàm giả lập logic đăng ký để Activity gọi
    public void registerUser(String fullName, String email, String password) {
        // Thực hiện logic kiểm tra (validate) dữ liệu tại đây
        // Gọi AuthRepository để giao tiếp với API backend
    }
}
