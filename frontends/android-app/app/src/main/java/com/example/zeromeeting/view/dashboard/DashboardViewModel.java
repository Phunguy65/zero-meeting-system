package com.example.zeromeeting.view.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.zeromeeting.core.model.user.UserResponse;
import com.example.zeromeeting.core.repository.UserRepository;
import com.example.zeromeeting.core.utils.Resource;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    private final UserRepository userRepository;

    @Inject
    public DashboardViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Hàm này sẽ được Fragment gọi để lấy data
    public LiveData<Resource<UserResponse>> getUserProfile() {
        return userRepository.getMe();
    }
}
