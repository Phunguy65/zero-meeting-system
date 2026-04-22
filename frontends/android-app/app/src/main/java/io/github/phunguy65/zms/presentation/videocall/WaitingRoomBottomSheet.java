package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.JoinRequestItem;
import io.github.phunguy65.zms.frontends.R;

/**
 * Bottom sheet dialog presenting pending join requests for the host.
 * Provides per-item approve/deny actions, an approve-all action,
 * and snackbar-based error feedback for API failures.
 */
@AndroidEntryPoint
public class WaitingRoomBottomSheet extends BottomSheetDialogFragment
        implements JoinRequestAdapter.ActionCallback {

    public static final String TAG = "WaitingRoomBottomSheet";

    private WaitingRoomViewModel viewModel;
    private CallViewModel callViewModel;
    private JoinRequestAdapter adapter;

    private View btnCloseContainer;
    private RecyclerView rvRequests;
    private MaterialButton btnApproveAll;
    private MaterialButton btnRetry;
    private ProgressBar progressLoading;
    private LinearLayout errorLayout;
    private TextView tvError;
    private TextView tvEmpty;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(WaitingRoomViewModel.class);
        callViewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialogInterface;
            FrameLayout bottomSheet = bottomSheetDialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.8));
            }
        });

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_waiting_room_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        setupObservers();

        loadCurrentRequests();
    }

    private void initViews(View view) {
        btnCloseContainer = view.findViewById(R.id.btnCloseContainer);
        rvRequests = view.findViewById(R.id.rvRequests);
        btnApproveAll = view.findViewById(R.id.btnApproveAll);
        btnRetry = view.findViewById(R.id.btnRetry);
        progressLoading = view.findViewById(R.id.progressLoading);
        errorLayout = view.findViewById(R.id.errorLayout);
        tvError = view.findViewById(R.id.tvError);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void setupRecyclerView() {
        rvRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new JoinRequestAdapter(this);
        rvRequests.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCloseContainer.setOnClickListener(v -> dismiss());

        btnApproveAll.setOnClickListener(v -> {
            String meetingId = callViewModel.getMeetingId().getValue();
            if (meetingId != null && !meetingId.isEmpty()) {
                viewModel.approveAll(meetingId);
            }
        });

        btnRetry.setOnClickListener(v -> loadCurrentRequests());
    }

    private void setupObservers() {
        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                progressLoading.setVisibility(View.VISIBLE);
                rvRequests.setVisibility(View.GONE);
                errorLayout.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                progressLoading.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                errorLayout.setVisibility(View.VISIBLE);
                tvError.setText(error);
                rvRequests.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.GONE);

                Snackbar.make(requireView(), error, Snackbar.LENGTH_SHORT).show();
            } else {
                errorLayout.setVisibility(View.GONE);
            }
        });

        viewModel.getActionError().observe(getViewLifecycleOwner(), actionError -> {
            if (actionError != null && !actionError.isEmpty()) {
                Snackbar.make(requireView(), actionError, Snackbar.LENGTH_SHORT).show();
                viewModel.clearActionError();
            }
        });

        viewModel.getJoinRequests().observe(getViewLifecycleOwner(), requests -> {
            if (requests == null) return;
            adapter.submitList(requests);

            boolean hasItems = !requests.isEmpty();
            rvRequests.setVisibility(hasItems ? View.VISIBLE : View.GONE);
            btnApproveAll.setEnabled(hasItems);
        });

        viewModel.isEmpty().observe(getViewLifecycleOwner(), isEmpty -> {
            Boolean loading = viewModel.isLoading().getValue();
            String error = viewModel.getError().getValue();
            boolean showEmpty = Boolean.TRUE.equals(isEmpty)
                    && !Boolean.TRUE.equals(loading)
                    && (error == null || error.isEmpty());
            tvEmpty.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        });

        viewModel.getApprovedOrDeniedRequestId().observe(getViewLifecycleOwner(), requestId -> {
            if (requestId != null && !requestId.isEmpty()) {
                callViewModel.removePendingRequest(requestId);
                viewModel.clearApprovedOrDeniedRequestId();
            }
        });

        viewModel.getApproveAllSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                callViewModel.clearAllPendingRequests();
                viewModel.clearApproveAllSuccess();
            }
        });
    }

    private void loadCurrentRequests() {
        String meetingId = callViewModel.getMeetingId().getValue();
        if (meetingId != null && !meetingId.isEmpty()) {
            viewModel.loadRequests(meetingId);
        }
    }

    @Override
    public void onApprove(JoinRequestItem item) {
        String meetingId = callViewModel.getMeetingId().getValue();
        if (meetingId != null && !meetingId.isEmpty()) {
            viewModel.approveRequest(meetingId, item.getId());
        }
    }

    @Override
    public void onDeny(JoinRequestItem item) {
        String meetingId = callViewModel.getMeetingId().getValue();
        if (meetingId != null && !meetingId.isEmpty()) {
            viewModel.denyRequest(meetingId, item.getId());
        }
    }
}
