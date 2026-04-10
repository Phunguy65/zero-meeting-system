package io.github.phunguy65.zms.view.profile;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    @Inject
    public ProfileViewModel() {
        // Sau này inject AuthRepository vào đây để xử lý hàm Log Out (xóa token)
    }

    public void logOut() {
        // Xóa thông tin đăng nhập trong SharedPreferences hoặc DataStore
    }
}
