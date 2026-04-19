package io.github.phunguy65.zms.presentation.main.calendar;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.domain.usecase.calendar.GetCalendarEventsUseCase;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * ViewModel for the Calendar screen.
 *
 * <p>Manages calendar state including selected date, current visible month,
 * month events (for day indicators), and selected-date events (for detail list).
 */
@HiltViewModel
public class CalendarViewModel extends ViewModel {

    private final GetCalendarEventsUseCase getCalendarEventsUseCase;
    private final Executor mainExecutor;

    private final MutableLiveData<LocalDate> _selectedDate = new MutableLiveData<>(LocalDate.now());
    public LiveData<LocalDate> selectedDate = _selectedDate;

    private final MutableLiveData<YearMonth> _currentMonth = new MutableLiveData<>(YearMonth.now());
    public LiveData<YearMonth> currentMonth = _currentMonth;

    private final MutableLiveData<Map<LocalDate, List<CalendarEvent>>> _monthEvents =
            new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<LocalDate, List<CalendarEvent>>> monthEvents = _monthEvents;

    private final MutableLiveData<List<CalendarEvent>> _selectedDateEvents =
            new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<CalendarEvent>> selectedDateEvents = _selectedDateEvents;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    @Inject
    public CalendarViewModel(
            GetCalendarEventsUseCase getCalendarEventsUseCase,
            @MainExecutor Executor mainExecutor) {
        this.getCalendarEventsUseCase = getCalendarEventsUseCase;
        this.mainExecutor = mainExecutor;

        loadEventsForMonth(YearMonth.now());
    }

    /**
     * Updates the selected date and refreshes the selected-date events.
     */
    public void selectDate(LocalDate date) {
        LocalDate previousDate = _selectedDate.getValue();
        _selectedDate.setValue(date);

        Map<LocalDate, List<CalendarEvent>> events = _monthEvents.getValue();
        if (events != null && events.containsKey(date)) {
            _selectedDateEvents.setValue(events.get(date));
        } else {
            _selectedDateEvents.setValue(new ArrayList<>());
        }
    }

    /**
     * Updates the current visible month and loads events for it.
     */
    public void setCurrentMonth(YearMonth month) {
        _currentMonth.setValue(month);
        loadEventsForMonth(month);
    }

    /**
     * Loads calendar events for the specified month.
     */
    public void loadEventsForMonth(YearMonth month) {
        _isLoading.setValue(true);

        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();
        ZoneId zoneId = ZoneId.systemDefault();

        OffsetDateTime start = startDate.atStartOfDay(zoneId).toOffsetDateTime();
        OffsetDateTime end = endDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

        getCalendarEventsUseCase
                .execute(start, end)
                .whenCompleteAsync(
                        (events, error) -> {
                            _isLoading.setValue(false);

                            if (error != null) {
                                _monthEvents.setValue(new HashMap<>());
                                _selectedDateEvents.setValue(new ArrayList<>());
                            } else {
                                Map<LocalDate, List<CalendarEvent>> eventsByDate = events.stream()
                                        .filter(e -> e.startTime() != null)
                                        .collect(Collectors.groupingBy(
                                                e -> e.startTime().atZoneSameInstant(zoneId).toLocalDate()));

                                _monthEvents.setValue(eventsByDate);

                                LocalDate selected = _selectedDate.getValue();
                                if (selected != null && eventsByDate.containsKey(selected)) {
                                    _selectedDateEvents.setValue(eventsByDate.get(selected));
                                } else if (selected != null) {
                                    _selectedDateEvents.setValue(new ArrayList<>());
                                }
                            }
                        },
                        mainExecutor);
    }

    /**
     * Returns the currently selected date value.
     */
    public LocalDate getSelectedDateValue() {
        return _selectedDate.getValue();
    }
}
