package com.example.zeromeeting.view.profile;

import androidx.lifecycle.ViewModel;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

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
