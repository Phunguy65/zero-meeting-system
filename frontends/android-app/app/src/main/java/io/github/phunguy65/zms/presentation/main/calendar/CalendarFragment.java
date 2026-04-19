package io.github.phunguy65.zms.presentation.main.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.CalendarMonth;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.CalendarView;
import com.kizitonwose.calendar.view.MonthDayBinder;
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.CalendarEvent;
import io.github.phunguy65.zms.frontends.R;
import java.time.DayOfWeek;
import kotlin.Unit;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Calendar fragment displaying meeting calendar with month navigation and day selection.
 *
 * <p>Shows a CalendarView with event indicators on days with meetings,
 * and displays selected-date events in a list below the calendar.
 */
@AndroidEntryPoint
public class CalendarFragment extends Fragment {

    private static final int MONTHS_RANGE = 100;

    private CalendarViewModel viewModel;

    private TextView tvMonthTitle;
    private CalendarView calendarView;
    private TextView tvSelectedDateHeader;
    private View layoutEmptyState;
    private RecyclerView rvEvents;

    private CalendarEventAdapter eventAdapter;
    private LocalDate selectedDate = LocalDate.now();

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        initViews(view);
        setupCalendar();
        setupEventsRecyclerView();
        setupObservers();
    }

    private void initViews(View view) {
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDateHeader = view.findViewById(R.id.tvSelectedDateHeader);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        rvEvents = view.findViewById(R.id.rvEvents);
    }

    private void setupCalendar() {
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(MONTHS_RANGE);
        YearMonth endMonth = currentMonth.plusMonths(MONTHS_RANGE);
        DayOfWeek firstDayOfWeek = DayOfWeek.MONDAY;

        calendarView.setDayBinder(new MonthDayBinder<DayViewContainer>() {
            @NonNull @Override
            public DayViewContainer create(@NonNull View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(@NonNull DayViewContainer container, @NonNull CalendarDay day) {
                LocalDate date = day.getDate();
                container.tvDayNumber.setText(String.valueOf(date.getDayOfMonth()));

                boolean isSelected = date.equals(selectedDate);

                boolean isInMonth = day.getPosition() == DayPosition.MonthDate;

                if (isInMonth) {
                    container.tvDayNumber.setVisibility(View.VISIBLE);

                    if (isSelected) {
                        container.tvDayNumber.setBackgroundResource(R.drawable.bg_circle_blue);
                        container.tvDayNumber.setTextColor(
                                requireContext().getColor(com.google.android.material.R.color.m3_ref_palette_white));
                    } else {
                        container.tvDayNumber.setBackground(null);
                        container.tvDayNumber.setTextColor(
                                requireContext().getColor(com.google.android.material.R.color.m3_ref_palette_black));
                    }

                    Map<LocalDate, List<CalendarEvent>> events = viewModel.monthEvents.getValue();
                    boolean hasEvents = events != null && events.containsKey(date)
                            && !events.get(date).isEmpty();
                    container.viewEventDot.setVisibility(hasEvents ? View.VISIBLE : View.INVISIBLE);

                    container.getView().setOnClickListener(v -> onDayClicked(date));
                } else {
                    container.tvDayNumber.setVisibility(View.INVISIBLE);
                    container.viewEventDot.setVisibility(View.INVISIBLE);
                    container.getView().setOnClickListener(null);
                }
            }
        });

        String[] weekdayAbbreviations = getResources().getStringArray(R.array.weekday_abbreviations);
        calendarView.setMonthHeaderBinder(new MonthHeaderFooterBinder<HeaderViewContainer>() {
            @NonNull @Override
            public HeaderViewContainer create(@NonNull View view) {
                return new HeaderViewContainer(view);
            }

            @Override
            public void bind(@NonNull HeaderViewContainer container, @NonNull CalendarMonth month) {
                for (int i = 0; i < container.weekdayLabels.length && i < weekdayAbbreviations.length; i++) {
                    container.weekdayLabels[i].setText(weekdayAbbreviations[i]);
                }
            }
        });

        calendarView.setMonthScrollListener(month -> {
            YearMonth yearMonth = month.getYearMonth();
            updateMonthTitle(yearMonth);
            viewModel.setCurrentMonth(yearMonth);
            return Unit.INSTANCE;
        });

        calendarView.setup(startMonth, endMonth, firstDayOfWeek);
        calendarView.scrollToMonth(currentMonth);

        updateMonthTitle(currentMonth);
    }

    private void setupEventsRecyclerView() {
        eventAdapter = new CalendarEventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(eventAdapter);
    }

    private void updateMonthTitle(YearMonth month) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText(month.format(formatter));
    }

    private void onDayClicked(LocalDate date) {
        LocalDate previousDate = selectedDate;
        selectedDate = date;

        calendarView.notifyDateChanged(previousDate);
        calendarView.notifyDateChanged(date);

        viewModel.selectDate(date);
    }

    private void setupObservers() {
        viewModel.selectedDate.observe(getViewLifecycleOwner(), date -> {
            if (date != null && !date.equals(selectedDate)) {
                LocalDate previousDate = selectedDate;
                selectedDate = date;
                calendarView.notifyDateChanged(previousDate);
                calendarView.notifyDateChanged(date);
            }
            updateSelectedDateHeader(date);
        });

        viewModel.monthEvents.observe(getViewLifecycleOwner(), events -> {
            YearMonth month = viewModel.currentMonth.getValue();
            if (month != null) {
                calendarView.notifyMonthChanged(month);
            }
        });

        viewModel.selectedDateEvents.observe(getViewLifecycleOwner(), this::updateEventsSection);
    }

    private void updateSelectedDateHeader(LocalDate date) {
        if (date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    .withLocale(Locale.getDefault());
            tvSelectedDateHeader.setText(date.format(formatter));
        }
    }

    private void updateEventsSection(List<CalendarEvent> events) {
        if (events == null || events.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvEvents.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvEvents.setVisibility(View.VISIBLE);
            eventAdapter.submitList(events);
        }
    }
}
