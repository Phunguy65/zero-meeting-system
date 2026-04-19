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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.state.UiState;
import io.github.phunguy65.zms.presentation.videocall.VideoCallActivity;
import java.util.List;

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
    private View layoutEmptyState;
    private RecyclerView rvUpcomingMeetings;
    private MaterialButton btnScheduleNew;
    private FloatingActionButton fabNewMeeting;

    private UpcomingMeetingAdapter adapter;

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
        setupRecyclerView();
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        cardJoinMeeting = view.findViewById(R.id.cardJoinMeeting);
        cardSchedule = view.findViewById(R.id.cardSchedule);
        btnSettings = view.findViewById(R.id.btnSettings);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        rvUpcomingMeetings = view.findViewById(R.id.rvUpcomingMeetings);
        btnScheduleNew = view.findViewById(R.id.btnScheduleNew);
        fabNewMeeting = view.findViewById(R.id.fabNewMeeting);
    }

    private void setupRecyclerView() {
        adapter = new UpcomingMeetingAdapter(this::onJoinMeetingClicked);
        rvUpcomingMeetings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvUpcomingMeetings.setAdapter(adapter);
    }

    private void onJoinMeetingClicked(@NonNull UpcomingMeeting meeting) {
        if (meeting.shortCode() != null) {
            launchVideoCall(meeting.shortCode());
        }
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

        viewModel.upcomingMeetingsState.observe(getViewLifecycleOwner(), this::renderUpcomingMeetings);
    }

    private void renderUpcomingMeetings(UiState<List<UpcomingMeeting>> state) {
        switch (state) {
            case UiState.Loading<List<UpcomingMeeting>> loading -> {
                layoutEmptyState.setVisibility(View.GONE);
                rvUpcomingMeetings.setVisibility(View.GONE);
            }
            case UiState.Success<List<UpcomingMeeting>> success -> {
                List<UpcomingMeeting> meetings = success.data();
                if (meetings == null || meetings.isEmpty()) {
                    layoutEmptyState.setVisibility(View.VISIBLE);
                    rvUpcomingMeetings.setVisibility(View.GONE);
                } else {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvUpcomingMeetings.setVisibility(View.VISIBLE);
                    adapter.submitList(meetings);
                }
            }
            case UiState.Error<List<UpcomingMeeting>> error -> {
                layoutEmptyState.setVisibility(View.VISIBLE);
                rvUpcomingMeetings.setVisibility(View.GONE);
                if (error.error() != null) {
                    Snackbar.make(requireView(), R.string.error_server, Snackbar.LENGTH_LONG)
                            .setAction(R.string.retry, v -> viewModel.loadUpcomingMeetings())
                            .show();
                }
            }
            case UiState.Idle<List<UpcomingMeeting>> idle -> {
            }
        }
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
}
