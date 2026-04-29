package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.meeting.participant.ParticipantAdapter;
import io.github.phunguy65.zms.presentation.meeting.participant.ParticipantsViewModel;

/**
 * Bottom sheet dialog for displaying meeting participants.
 * Replaces ParticipantsActivity for use within VideoCallActivity.
 */
@AndroidEntryPoint
public class ParticipantsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "ParticipantsBottomSheet";

    private ParticipantsViewModel viewModel;
    private CallViewModel callViewModel;
    private ParticipantAdapter adapter;

    // Views
    private View btnCloseContainer;
    private TextView tvTitle;
    private RecyclerView rvParticipants;
    private MaterialButton btnMuteAll;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ParticipantsViewModel.class);
        callViewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
    }

    @NonNull @Override
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
                // Set to 80% of screen height
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.8));
            }
        });

        return dialog;
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_participants_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        setupListeners();
        setupObservers();

        String meetingId = callViewModel.getMeetingId().getValue();
        if (meetingId != null && !meetingId.isEmpty()) {
            viewModel.enrichWithRoles(meetingId);
        }
    }

    private void initViews(View view) {
        btnCloseContainer = view.findViewById(R.id.btnCloseContainer);
        tvTitle = view.findViewById(R.id.tvTitle);
        rvParticipants = view.findViewById(R.id.rvParticipants);
        btnMuteAll = view.findViewById(R.id.btnMuteAll);
    }

    private void setupRecyclerView() {
        rvParticipants.setLayoutManager(new LinearLayoutManager(requireContext()));
        // Create adapter once with empty list
        adapter = new ParticipantAdapter(new java.util.ArrayList<>());
        rvParticipants.setAdapter(adapter);
    }

    private void setupListeners() {
        btnCloseContainer.setOnClickListener(v -> dismiss());

        // TODO: Enable mute-all when backend muteAllParticipants API is implemented
        btnMuteAll.setEnabled(false);
        btnMuteAll.setVisibility(View.GONE);
    }

    private void setupObservers() {
        callViewModel
                .getParticipants()
                .observe(
                        getViewLifecycleOwner(),
                        videoParticipants -> viewModel.setLiveKitParticipants(videoParticipants));

        viewModel.getParticipants().observe(getViewLifecycleOwner(), participants -> {
            adapter.updateList(participants);

            String title = getString(R.string.call_participants_count, participants.size());
            tvTitle.setText(title);
        });
    }
}
