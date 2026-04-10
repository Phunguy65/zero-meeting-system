package io.github.phunguy65.zms.presentation.auth.register;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

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
