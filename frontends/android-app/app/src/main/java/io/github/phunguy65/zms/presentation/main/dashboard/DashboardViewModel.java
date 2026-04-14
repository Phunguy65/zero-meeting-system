package io.github.phunguy65.zms.presentation.main.dashboard;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class DashboardViewModel extends ViewModel {

    @Inject
    public DashboardViewModel() {
        // TODO: Inject Repository to fetch meetings list
    }
}
