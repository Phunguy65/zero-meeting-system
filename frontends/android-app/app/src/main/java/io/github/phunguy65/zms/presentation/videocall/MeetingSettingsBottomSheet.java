package io.github.phunguy65.zms.presentation.videocall;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.frontends.R;

/**
 * Bottom sheet dialog for host-only meeting settings management.
 * Allows hosts to modify meeting settings during a live session.
 */
@AndroidEntryPoint
public class MeetingSettingsBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MeetingSettingsBottomSheet";

    private CallViewModel viewModel;

    private CircularProgressIndicator progressLoading;
    private MaterialSwitch switchWaitingRoom;
    private MaterialSwitch switchAllowGuest;
    private MaterialSwitch switchChatEnabled;
    private MaterialSwitch switchAllowScreenShare;
    private MaterialButton btnApplySettings;

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
        return inflater.inflate(R.layout.bottom_sheet_meeting_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupListeners();
        setupObservers();

        viewModel.loadMeetingSettings();
    }

    private void initViews(View view) {
        progressLoading = view.findViewById(R.id.progressLoading);
        switchWaitingRoom = view.findViewById(R.id.switchWaitingRoom);
        switchAllowGuest = view.findViewById(R.id.switchAllowGuest);
        switchChatEnabled = view.findViewById(R.id.switchChatEnabled);
        switchAllowScreenShare = view.findViewById(R.id.switchAllowScreenShare);
        btnApplySettings = view.findViewById(R.id.btnApplySettings);
    }

    private void setupListeners() {
        btnApplySettings.setOnClickListener(v -> applySettings());
    }

    private void setupObservers() {
        viewModel.getMeetingSettings().observe(getViewLifecycleOwner(), settings -> {
            if (settings != null) {
                populateSettings(settings);
            }
        });

        viewModel.isSettingsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setControlsEnabled(!isLoading);
        });

        viewModel.getSettingsError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(
                                requireView(),
                                R.string.meeting_settings_update_error,
                                Snackbar.LENGTH_LONG)
                        .show();
            }
        });

        viewModel.getSettingsUpdateSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                viewModel.clearSettingsUpdateSuccess();
                Snackbar.make(
                                requireView(),
                                R.string.meeting_settings_update_success,
                                Snackbar.LENGTH_SHORT)
                        .show();
                dismiss();
            }
        });
    }

    private void populateSettings(MeetingSettings settings) {
        switchWaitingRoom.setChecked(settings.isWaitingRoomEnabled());
        switchAllowGuest.setChecked(settings.isAllowGuest());
        switchChatEnabled.setChecked(settings.isChatEnabled());
        switchAllowScreenShare.setChecked(settings.isAllowScreenShare());
    }

    private void applySettings() {
        MeetingSettings currentSettings = viewModel.getMeetingSettings().getValue();
        if (currentSettings == null) {
            return;
        }

        MeetingSettings newSettings = currentSettings.toBuilder()
                .waitingRoomEnabled(switchWaitingRoom.isChecked())
                .allowGuest(switchAllowGuest.isChecked())
                .chatEnabled(switchChatEnabled.isChecked())
                .allowScreenShare(switchAllowScreenShare.isChecked())
                .build();

        viewModel.updateMeetingSettings(newSettings);
    }

    private void setControlsEnabled(boolean enabled) {
        switchWaitingRoom.setEnabled(enabled);
        switchAllowGuest.setEnabled(enabled);
        switchChatEnabled.setEnabled(enabled);
        switchAllowScreenShare.setEnabled(enabled);
        btnApplySettings.setEnabled(enabled);
    }
}
