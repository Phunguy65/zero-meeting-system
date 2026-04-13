package io.github.phunguy65.zms.presentation.dashboard;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    @Inject
    public DashboardViewModel() {
        // Sau này inject Repository để gọi API lấy danh sách Meeting
    }
}
