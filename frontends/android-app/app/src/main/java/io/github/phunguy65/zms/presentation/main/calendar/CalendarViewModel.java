package io.github.phunguy65.zms.presentation.main.calendar;

import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class CalendarViewModel extends ViewModel {

    @Inject
    public CalendarViewModel() {
        // TODO: Inject Repository to fetch calendar events by date
    }
}
