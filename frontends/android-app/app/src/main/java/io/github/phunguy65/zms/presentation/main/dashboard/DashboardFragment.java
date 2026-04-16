package io.github.phunguy65.zms.presentation.main.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Dashboard fragment displaying quick actions and upcoming meetings.
 *
 * <p>This fragment is the start destination in nav_graph_main and shows:
 * - Quick action cards (New Meeting, Join Meeting, Schedule)
 * - Upcoming meetings list (or empty state when no meetings)
 * - Settings button in the header
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private NavController navController;

    private MaterialCardView cardNewMeeting, cardJoinMeeting, cardSchedule;
    private ImageView btnSettings;
    private View layoutEmptyState, meetingsContainer;
    private MaterialButton btnScheduleNew;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);
        navController = NavHostFragment.findNavController(this);

        initViews(view);
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        cardNewMeeting = view.findViewById(R.id.cardNewMeeting);
        cardJoinMeeting = view.findViewById(R.id.cardJoinMeeting);
        cardSchedule = view.findViewById(R.id.cardSchedule);
        btnSettings = view.findViewById(R.id.btnSettings);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        meetingsContainer = view.findViewById(R.id.meetingsContainer);
        btnScheduleNew = view.findViewById(R.id.btnScheduleNew);
    }

    private void setupListeners() {
        cardNewMeeting.setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboard_to_createMeeting);
        });

        cardJoinMeeting.setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboard_to_joinMeeting);
        });

        cardSchedule.setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboard_to_schedule);
        });

        btnSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboard_to_settings);
        });

        if (btnScheduleNew != null) {
            btnScheduleNew.setOnClickListener(v -> {
                navController.navigate(R.id.action_dashboard_to_schedule);
            });
        }
    }

    private void setupObservers() {
        // Observe upcoming meetings from ViewModel
        // When implemented, toggle empty state based on meetings list
        // For now, show meetings (placeholder data is in the layout)
        updateEmptyState(false);
    }

    /**
     * Toggle between empty state and meetings list.
     * @param isEmpty true to show empty state, false to show meetings
     */
    private void updateEmptyState(boolean isEmpty) {
        if (layoutEmptyState != null && meetingsContainer != null) {
            layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            meetingsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }
}
