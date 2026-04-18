package io.github.phunguy65.zms.presentation.main.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.androidveil.VeilRecyclerFrameView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Fragment displaying the authenticated user's meeting history with skeleton loading, infinite
 * scroll pagination, pull-to-refresh, empty, and error states.
 */
@AndroidEntryPoint
public class MeetingHistoryFragment extends Fragment {

    private static final int PAGINATION_PREFETCH_DISTANCE = 3;

    private MeetingHistoryViewModel viewModel;
    private NavController navController;

    private VeilRecyclerFrameView veilRecyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private View layoutErrorState;
    private MaterialButton btnRetry;
    private MaterialButton btnEmptyStartMeeting;
    private ImageView btnBack;
    private FrameLayout btnBackWrapper;

    private MeetingHistoryAdapter adapter;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meeting_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(MeetingHistoryViewModel.class);
        navController = NavHostFragment.findNavController(this);

        initViews(view);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        veilRecyclerView = view.findViewById(R.id.veilRecyclerView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        layoutErrorState = view.findViewById(R.id.layoutErrorState);
        btnRetry = view.findViewById(R.id.btnRetry);
        btnEmptyStartMeeting = layoutEmptyState.findViewById(R.id.btnEmptyStartMeeting);
        btnBack = view.findViewById(R.id.btnBack);
        btnBackWrapper = view.findViewById(R.id.btnBackWrapper);
    }

    private void setupRecyclerView() {
        adapter = new MeetingHistoryAdapter(this::onMeetingClicked);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        veilRecyclerView.setLayoutManager(layoutManager);
        veilRecyclerView.setAdapter(adapter);
        veilRecyclerView.addVeiledItems(5);

        veilRecyclerView
                .getRecyclerView()
                .addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        if (dy <= 0) return;
                        int total = layoutManager.getItemCount();
                        int lastVisible = layoutManager.findLastVisibleItemPosition();
                        if (lastVisible >= total - PAGINATION_PREFETCH_DISTANCE) {
                            viewModel.loadMore();
                        }
                    }
                });
    }

    private void setupListeners() {
        View.OnClickListener back = v -> Navigation.findNavController(v).popBackStack();
        btnBack.setOnClickListener(back);
        btnBackWrapper.setOnClickListener(back);

        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());

        btnRetry.setOnClickListener(v -> viewModel.loadInitial());

        btnEmptyStartMeeting.setOnClickListener(v -> {
            navController.popBackStack(R.id.dashboardFragment, false);
        });
    }

    private void observeViewModel() {
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);

        viewModel.getPageErrorEvent()
                .observe(
                        getViewLifecycleOwner(),
                        message -> {
                            if (message == null || message.isEmpty()) return;
                            Snackbar.make(
                                            requireView(),
                                            R.string.meeting_history_load_more_failed,
                                            Snackbar.LENGTH_LONG)
                                    .setAction(R.string.retry, v -> viewModel.loadMore())
                                    .show();
                            viewModel.consumePageError();
                        });
    }

    private void render(MeetingHistoryUiState state) {
        switch (state) {
            case MeetingHistoryUiState.Loading loading -> showLoading();
            case MeetingHistoryUiState.Success success -> showSuccess(success);
            case MeetingHistoryUiState.Empty empty -> showEmpty();
            case MeetingHistoryUiState.Error error -> showError();
        }
    }

    private void showLoading() {
        swipeRefresh.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        veilRecyclerView.veil();
    }

    private void showSuccess(MeetingHistoryUiState.Success state) {
        swipeRefresh.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        veilRecyclerView.unVeil();
        swipeRefresh.setRefreshing(state.isRefreshing());
        adapter.submitList(state.items());
    }

    private void showEmpty() {
        swipeRefresh.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.VISIBLE);
        veilRecyclerView.unVeil();
        swipeRefresh.setRefreshing(false);
    }

    private void showError() {
        swipeRefresh.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);
        layoutErrorState.setVisibility(View.VISIBLE);
        veilRecyclerView.unVeil();
        swipeRefresh.setRefreshing(false);
    }

    private void onMeetingClicked(@NonNull io.github.phunguy65.zms.domain.model.MeetingHistory item) {
        Bundle args = new Bundle();
        args.putString("meetingId", item.id());
        navController.navigate(R.id.action_meetingHistory_to_meetingDetail, args);
    }
}
