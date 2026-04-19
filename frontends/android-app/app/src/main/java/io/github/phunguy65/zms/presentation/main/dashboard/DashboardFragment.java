package io.github.phunguy65.zms.presentation.main.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.videocall.VideoCallActivity;

/**
 * Dashboard fragment displaying quick actions and upcoming meetings.
 *
 * <p>This fragment is the start destination in nav_graph_main and shows:
 * - FAB for starting instant meeting or scheduling
 * - Quick action cards (Join Meeting, Schedule)
 * - Upcoming meetings list (or empty state when no meetings)
 * - Settings button in the header
 */
@AndroidEntryPoint
public class DashboardFragment extends Fragment {

    private DashboardViewModel viewModel;
    private NavController navController;

    private MaterialCardView cardJoinMeeting, cardSchedule;
    private ImageView btnSettings;
    private View layoutEmptyState, meetingsContainer;
    private MaterialButton btnScheduleNew;
    private FloatingActionButton fabNewMeeting;

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
        cardJoinMeeting = view.findViewById(R.id.cardJoinMeeting);
        cardSchedule = view.findViewById(R.id.cardSchedule);
        btnSettings = view.findViewById(R.id.btnSettings);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        meetingsContainer = view.findViewById(R.id.meetingsContainer);
        btnScheduleNew = view.findViewById(R.id.btnScheduleNew);
        fabNewMeeting = view.findViewById(R.id.fabNewMeeting);
    }

    private void setupListeners() {
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

        fabNewMeeting.setOnClickListener(this::showFabMenu);
    }

    /**
     * Shows popup menu anchored to the FAB with instant meeting and schedule options.
     */
    private void showFabMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_fab_new_meeting, popup.getMenu());

        popup.setOnMenuItemClickListener(this::onFabMenuItemClick);
        popup.show();
    }

    /**
     * Handles FAB menu item clicks.
     */
    private boolean onFabMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_start_instant) {
            viewModel.createInstantMeeting();
            return true;
        } else if (itemId == R.id.action_schedule_meeting) {
            navController.navigate(R.id.action_dashboard_to_schedule);
            return true;
        }

        return false;
    }

    private void setupObservers() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            fabNewMeeting.setEnabled(!isLoading);
            if (isLoading) {
                Snackbar.make(
                                requireView(),
                                R.string.meeting_creation_loading,
                                Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        viewModel.instantMeetingSuccess.observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.getShortCode() != null) {
                launchVideoCall(result.getShortCode());
            }
        });

        viewModel.instantMeetingError.observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });

        updateEmptyState(false);
    }

    /**
     * Launches VideoCallActivity with the created meeting short code.
     * Uses FLAG_ACTIVITY_NEW_TASK as per spec for separate task.
     */
    private void launchVideoCall(String meetingCode) {
        Intent intent = new Intent(requireContext(), VideoCallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(VideoCallActivity.EXTRA_MEETING_CODE, meetingCode);
        intent.putExtra(VideoCallActivity.EXTRA_IS_GUEST, false);
        startActivity(intent);
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
