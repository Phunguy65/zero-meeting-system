package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.VideoLayout;
import io.github.phunguy65.zms.frontends.R;

/**
 * Bottom sheet dialog for secondary call actions.
 * Contains screen share, chat, participants, layout selection, and host-only settings.
 */
@AndroidEntryPoint
public class MeetingActionsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MeetingActionsBottomSheet";

    public interface ActionsCallback {
        void onScreenShareClicked();

        void onChatClicked();

        void onParticipantsClicked();

        void onChangeLayoutClicked();

        void onMeetingSettingsClicked();
    }

    private CallViewModel viewModel;
    private ActionsCallback callback;

    private LinearLayout rowScreenShare;
    private LinearLayout rowChat;
    private LinearLayout rowParticipants;
    private LinearLayout rowChangeLayout;
    private LinearLayout rowMeetingSettings;
    private View dividerSettings;
    private TextView tvParticipantCount;
    private TextView tvCurrentLayout;

    public void setCallback(ActionsCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(CallViewModel.class);
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
            }
        });

        return dialog;
    }

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_meeting_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        setupObservers();
    }

    private void initViews(View view) {
        rowScreenShare = view.findViewById(R.id.rowScreenShare);
        rowChat = view.findViewById(R.id.rowChat);
        rowParticipants = view.findViewById(R.id.rowParticipants);
        rowChangeLayout = view.findViewById(R.id.rowChangeLayout);
        rowMeetingSettings = view.findViewById(R.id.rowMeetingSettings);
        dividerSettings = view.findViewById(R.id.dividerSettings);
        tvParticipantCount = view.findViewById(R.id.tvParticipantCount);
        tvCurrentLayout = view.findViewById(R.id.tvCurrentLayout);
    }

    private void setupListeners() {
        rowScreenShare.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onScreenShareClicked();
            } else {
                Snackbar.make(requireView(), R.string.feature_coming_soon, Snackbar.LENGTH_SHORT)
                        .show();
            }
        });

        rowChat.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onChatClicked();
            }
        });

        rowParticipants.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onParticipantsClicked();
            }
        });

        rowChangeLayout.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onChangeLayoutClicked();
            }
        });

        rowMeetingSettings.setOnClickListener(v -> {
            dismiss();
            if (callback != null) {
                callback.onMeetingSettingsClicked();
            }
        });
    }

    private void setupObservers() {
        viewModel.getParticipants().observe(getViewLifecycleOwner(), participants -> {
            int count = participants.size() + 1;
            tvParticipantCount.setText(String.valueOf(count));
        });

        viewModel.getCurrentLayout().observe(getViewLifecycleOwner(), layout -> {
            tvCurrentLayout.setText(getLayoutDisplayName(layout));
        });

        viewModel.isHost().observe(getViewLifecycleOwner(), isHost -> {
            int visibility = isHost ? View.VISIBLE : View.GONE;
            rowMeetingSettings.setVisibility(visibility);
            dividerSettings.setVisibility(visibility);
        });
    }

    private String getLayoutDisplayName(VideoLayout layout) {
        if (layout == null) {
            return getString(R.string.layout_auto);
        }
        return switch (layout) {
            case AUTO -> getString(R.string.layout_auto);
            case TILED -> getString(R.string.layout_tiled);
            case SPOTLIGHT -> getString(R.string.layout_spotlight);
            case SIDEBAR -> getString(R.string.layout_sidebar);
        };
    }
}
