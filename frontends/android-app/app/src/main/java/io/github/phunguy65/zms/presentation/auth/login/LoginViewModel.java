package io.github.phunguy65.zms.presentation.auth.login;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

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
