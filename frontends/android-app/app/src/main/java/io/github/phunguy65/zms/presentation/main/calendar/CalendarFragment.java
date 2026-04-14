package io.github.phunguy65.zms.presentation.main.calendar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Calendar fragment displaying meeting calendar with day/week views.
 *
 * <p>Shows a calendar strip with dates and meetings for the selected day.
 * Displays an empty state when no events are scheduled for the selected day.
 */
@AndroidEntryPoint
public class CalendarFragment extends Fragment {

    private CalendarViewModel viewModel;
    private View layoutEmptyState, eventsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        initViews(view);
        setupObservers();
    }

    private void initViews(View view) {
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        eventsContainer = view.findViewById(R.id.eventsContainer);
    }

    private void setupObservers() {
        // Observe events from ViewModel using getViewLifecycleOwner() to prevent memory leaks
        // viewModel.getEvents().observe(getViewLifecycleOwner(), events -> {
        //     updateEmptyState(events.isEmpty());
        // });

        // For now, show events (placeholder data is in the layout)
        updateEmptyState(false);
    }

    /**
     * Toggle between empty state and events list.
     * @param isEmpty true to show empty state, false to show events
     */
    private void updateEmptyState(boolean isEmpty) {
        if (layoutEmptyState != null && eventsContainer != null) {
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            eventsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}
