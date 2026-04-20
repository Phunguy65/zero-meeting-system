package io.github.phunguy65.zms.presentation.main.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.schedule.ScheduleViewModel;
import io.github.phunguy65.zms.presentation.schedule.ScheduleViewModel.ValidationResult;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment for scheduling a new meeting or editing an existing meeting's settings.
 *
 * <p>Supports two modes:
 * - Create mode (default): Schedule a new meeting with all fields editable.
 * - Edit mode (when meetingId argument present): Update pre-meeting settings only.
 *   Date/time fields are read-only in edit mode since backend doesn't support
 *   metadata updates.
 */
@AndroidEntryPoint
public class ScheduleFragment extends Fragment {

    private ScheduleViewModel viewModel;

    // Header
    private ImageView btnBack;
    private TextView tvTitle;

    // Basic info fields
    private TextInputLayout tilMeetingTopic, tilDate, tilTime, tilDuration;
    private TextInputEditText edtMeetingTopic, edtDate, edtTime;
    private AutoCompleteTextView tvDuration;
    private TextView tvEndTimeHelper;

    // Primary settings
    private MaterialSwitch switchWaitingRoom, switchAllowGuest, switchPassword;
    private TextInputLayout tilPassword;
    private TextInputEditText edtPassword;

    // Advanced settings
    private LinearLayout advancedSettingsHeader, advancedSettingsContent;
    private ImageView ivAdvancedExpand;
    private TextInputLayout tilMaxParticipants;
    private TextInputEditText edtMaxParticipants;
    private MaterialSwitch switchAllowScreenShare,
            switchChatEnabled,
            switchAllowMicrophone,
            switchAllowVideo;

    // Submit
    private MaterialButton btnScheduleMeeting;
    private CircularProgressIndicator progressLoading;

    private boolean advancedExpanded = false;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);

        initViews(view);
        setupDropdowns();
        setupListeners();
        setupBlurValidation();
        setupObservers();

        String meetingId = getArguments() != null ? getArguments().getString("meetingId") : null;
        viewModel.initEditMode(meetingId);
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvTitle = view.findViewById(R.id.tvTitle);

        // Basic info
        tilMeetingTopic = view.findViewById(R.id.tilMeetingTopic);
        edtMeetingTopic = view.findViewById(R.id.edtMeetingTopic);
        tilDate = view.findViewById(R.id.tilDate);
        edtDate = view.findViewById(R.id.edtDate);
        tilTime = view.findViewById(R.id.tilTime);
        edtTime = view.findViewById(R.id.edtTime);
        tilDuration = view.findViewById(R.id.tilDuration);
        tvDuration = view.findViewById(R.id.tvDuration);
        tvEndTimeHelper = view.findViewById(R.id.tvEndTimeHelper);

        // Primary settings
        switchWaitingRoom = view.findViewById(R.id.switchWaitingRoom);
        switchAllowGuest = view.findViewById(R.id.switchAllowGuest);
        switchPassword = view.findViewById(R.id.switchPassword);
        tilPassword = view.findViewById(R.id.tilPassword);
        edtPassword = view.findViewById(R.id.edtPassword);

        // Advanced settings
        advancedSettingsHeader = view.findViewById(R.id.advancedSettingsHeader);
        advancedSettingsContent = view.findViewById(R.id.advancedSettingsContent);
        ivAdvancedExpand = view.findViewById(R.id.ivAdvancedExpand);
        tilMaxParticipants = view.findViewById(R.id.tilMaxParticipants);
        edtMaxParticipants = view.findViewById(R.id.edtMaxParticipants);
        switchAllowScreenShare = view.findViewById(R.id.switchAllowScreenShare);
        switchChatEnabled = view.findViewById(R.id.switchChatEnabled);
        switchAllowMicrophone = view.findViewById(R.id.switchAllowMicrophone);
        switchAllowVideo = view.findViewById(R.id.switchAllowVideo);

        // Submit
        btnScheduleMeeting = view.findViewById(R.id.btnScheduleMeeting);
        progressLoading = view.findViewById(R.id.progressLoading);
    }

    private void setupDropdowns() {
        String[] durations = getResources().getStringArray(R.array.schedule_durations);
        ArrayAdapter<String> durationAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, durations);
        tvDuration.setAdapter(durationAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        edtDate.setOnClickListener(v -> showDatePicker());
        edtTime.setOnClickListener(v -> showTimePicker());

        switchPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilPassword.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            viewModel.setPasswordEnabled(isChecked);
            if (!isChecked && edtPassword != null) {
                edtPassword.setText("");
            }
        });

        switchAllowGuest.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setAllowGuest(isChecked));

        switchAllowScreenShare.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setAllowScreenShare(isChecked));

        switchChatEnabled.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setChatEnabled(isChecked));

        switchAllowMicrophone.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setAllowMicrophone(isChecked));

        switchAllowVideo.setOnCheckedChangeListener(
                (buttonView, isChecked) -> viewModel.setAllowVideo(isChecked));

        advancedSettingsHeader.setOnClickListener(v -> toggleAdvancedSettings());

        tvDuration.setOnItemClickListener((parent, view, position, id) -> updateEndTimeHelper());

        btnScheduleMeeting.setOnClickListener(v -> {
            boolean isWaitingRoom = switchWaitingRoom.isChecked();
            String password = switchPassword.isChecked() && edtPassword.getText() != null
                    ? edtPassword.getText().toString()
                    : null;

            if (edtMaxParticipants.getText() != null) {
                String maxParticipantsStr =
                        edtMaxParticipants.getText().toString().trim();
                try {
                    int maxParticipants = Integer.parseInt(maxParticipantsStr);
                    viewModel.setMaxParticipants(maxParticipants);
                } catch (NumberFormatException e) {
                    tilMaxParticipants.setError(
                            getString(R.string.schedule_error_max_participants_invalid));
                    return;
                }
            }

            Boolean isEditMode = viewModel.isEditMode.getValue();
            if (isEditMode != null && isEditMode) {
                viewModel.updateMeetingSettings(isWaitingRoom, password);
            } else {
                String topic = edtMeetingTopic.getText() != null
                        ? edtMeetingTopic.getText().toString()
                        : "";
                String date =
                        edtDate.getText() != null ? edtDate.getText().toString().trim() : "";
                String time =
                        edtTime.getText() != null ? edtTime.getText().toString().trim() : "";
                String duration = tvDuration.getText() != null
                        ? tvDuration.getText().toString().trim()
                        : "";
                viewModel.scheduleMeeting(topic, date, time, duration, isWaitingRoom, password);
            }
        });
    }

    private void setupBlurValidation() {
        edtMeetingTopic.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String title = edtMeetingTopic.getText() != null
                        ? edtMeetingTopic.getText().toString()
                        : null;
                ValidationResult result = viewModel.validateTitle(title);
                if (!result.isValid && "TITLE_TOO_LONG".equals(result.errorCode)) {
                    tilMeetingTopic.setError(getString(R.string.schedule_error_title_too_long));
                } else {
                    tilMeetingTopic.setError(null);
                }
            }
        });

        edtDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String date = edtDate.getText() != null ? edtDate.getText().toString() : null;
                ValidationResult result = viewModel.validateDate(date);
                if (!result.isValid) {
                    tilDate.setError(getString(R.string.error_date_required));
                } else {
                    tilDate.setError(null);
                }
                updateEndTimeHelper();
            }
        });

        edtTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String time = edtTime.getText() != null ? edtTime.getText().toString() : null;
                ValidationResult result = viewModel.validateTime(time);
                if (!result.isValid) {
                    tilTime.setError(getString(R.string.error_time_required));
                } else {
                    tilTime.setError(null);
                }
                updateEndTimeHelper();
            }
        });

        edtMaxParticipants.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String maxParticipants = edtMaxParticipants.getText() != null
                        ? edtMaxParticipants.getText().toString()
                        : null;
                ValidationResult result = viewModel.validateMaxParticipants(maxParticipants);
                if (!result.isValid) {
                    if ("INVALID_MAX_PARTICIPANTS_RANGE".equals(result.errorCode)) {
                        tilMaxParticipants.setError(
                                getString(R.string.schedule_error_max_participants_range));
                    } else {
                        tilMaxParticipants.setError(
                                getString(R.string.schedule_error_max_participants_invalid));
                    }
                } else {
                    tilMaxParticipants.setError(null);
                }
            }
        });

        tvDuration.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String duration =
                        tvDuration.getText() != null ? tvDuration.getText().toString() : null;
                ValidationResult result = viewModel.validateDuration(duration);
                if (!result.isValid) {
                    if ("INVALID_DURATION_RANGE".equals(result.errorCode)) {
                        tilDuration.setError(
                                getString(R.string.schedule_error_invalid_duration_range));
                    } else if ("EMPTY_DURATION".equals(result.errorCode)) {
                        tilDuration.setError(getString(R.string.error_duration_required));
                    } else {
                        tilDuration.setError(getString(R.string.schedule_error_invalid_duration));
                    }
                } else {
                    tilDuration.setError(null);
                }
                updateEndTimeHelper();
            }
        });

        edtPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String password =
                        edtPassword.getText() != null ? edtPassword.getText().toString() : null;
                ValidationResult result = viewModel.validatePassword(password);
                if (!result.isValid) {
                    tilPassword.setError(getString(R.string.validation_required));
                } else {
                    tilPassword.setError(null);
                }
            }
        });

        TextWatcher endTimeWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateEndTimeHelper();
            }
        };
        edtDate.addTextChangedListener(endTimeWatcher);
        edtTime.addTextChangedListener(endTimeWatcher);
        tvDuration.addTextChangedListener(endTimeWatcher);
    }

    private void setupObservers() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            Boolean isEditMode = viewModel.isEditMode.getValue();
            boolean inEditMode = isEditMode != null && isEditMode;

            btnScheduleMeeting.setEnabled(!isLoading);
            edtMeetingTopic.setEnabled(!isLoading && !inEditMode);
            edtDate.setEnabled(!isLoading && !inEditMode);
            edtTime.setEnabled(!isLoading && !inEditMode);
            tvDuration.setEnabled(!isLoading && !inEditMode);
            switchWaitingRoom.setEnabled(!isLoading);
            switchAllowGuest.setEnabled(!isLoading);
            switchPassword.setEnabled(!isLoading);
            edtPassword.setEnabled(!isLoading);
            edtMaxParticipants.setEnabled(!isLoading);
            switchAllowScreenShare.setEnabled(!isLoading);
            switchChatEnabled.setEnabled(!isLoading);
            switchAllowMicrophone.setEnabled(!isLoading);
            switchAllowVideo.setEnabled(!isLoading);

            if (isLoading) {
                btnScheduleMeeting.setText("");
                progressLoading.setVisibility(View.VISIBLE);
            } else {
                btnScheduleMeeting.setText(inEditMode
                        ? R.string.schedule_update_button
                        : R.string.schedule_button);
                progressLoading.setVisibility(View.GONE);
            }
        });

        viewModel.scheduleSuccess.observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Snackbar.make(
                                requireView(),
                                R.string.schedule_creation_success,
                                Snackbar.LENGTH_SHORT)
                        .show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        viewModel.scheduleError.observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.validationError.observe(getViewLifecycleOwner(), errorCode -> {
            if (errorCode == null) return;

            int messageResId;
            switch (errorCode) {
                case "TITLE_TOO_LONG":
                    messageResId = R.string.schedule_error_title_too_long;
                    break;
                case "EMPTY_DATE":
                    messageResId = R.string.error_date_required;
                    break;
                case "EMPTY_TIME":
                    messageResId = R.string.error_time_required;
                    break;
                case "EMPTY_DURATION":
                    messageResId = R.string.error_duration_required;
                    break;
                case "INVALID_DATE_TIME":
                    messageResId = R.string.validation_invalid_format;
                    break;
                case "PAST_START_TIME":
                    messageResId = R.string.schedule_error_past_start_time;
                    break;
                case "INVALID_DURATION":
                    messageResId = R.string.schedule_error_invalid_duration;
                    break;
                case "INVALID_DURATION_RANGE":
                    messageResId = R.string.schedule_error_invalid_duration_range;
                    break;
                case "INVALID_MAX_PARTICIPANTS_RANGE":
                    messageResId = R.string.schedule_error_max_participants_range;
                    break;
                default:
                    messageResId = R.string.validation_failed;
            }

            Snackbar.make(requireView(), messageResId, Snackbar.LENGTH_SHORT).show();
        });

        viewModel.endTimeText.observe(getViewLifecycleOwner(), endTime -> {
            if (endTime != null && !endTime.isEmpty()) {
                tvEndTimeHelper.setText(getString(R.string.schedule_end_time_helper, endTime));
                tvEndTimeHelper.setVisibility(View.VISIBLE);
            } else {
                tvEndTimeHelper.setVisibility(View.GONE);
            }
        });

        viewModel.isEditMode.observe(getViewLifecycleOwner(), isEditMode -> {
            if (isEditMode != null && isEditMode) {
                if (tvTitle != null) {
                    tvTitle.setText(R.string.schedule_edit_title);
                }
                btnScheduleMeeting.setText(R.string.schedule_update_button);
                edtMeetingTopic.setEnabled(false);
                edtDate.setEnabled(false);
                edtTime.setEnabled(false);
                tvDuration.setEnabled(false);
            }
        });

        viewModel.meetingDetail.observe(getViewLifecycleOwner(), this::populateEditModeFields);

        viewModel.loadDetailError.observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                Snackbar.make(requireView(), R.string.schedule_load_error, Snackbar.LENGTH_LONG)
                        .show();
            }
        });

        viewModel.updateSuccess.observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                Snackbar.make(requireView(), R.string.schedule_update_success, Snackbar.LENGTH_SHORT)
                        .show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    private void toggleAdvancedSettings() {
        advancedExpanded = !advancedExpanded;
        advancedSettingsContent.setVisibility(advancedExpanded ? View.VISIBLE : View.GONE);

        ivAdvancedExpand
                .animate()
                .rotation(advancedExpanded ? 90f : 0f)
                .setDuration(200)
                .start();
    }

    private void updateEndTimeHelper() {
        String date = edtDate.getText() != null ? edtDate.getText().toString() : "";
        String time = edtTime.getText() != null ? edtTime.getText().toString() : "";
        String duration = tvDuration.getText() != null ? tvDuration.getText().toString() : "";

        viewModel.updateEndTime(date, time, duration);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(
                            Locale.ROOT,
                            "%02d/%02d/%04d",
                            selectedMonth + 1,
                            selectedDay,
                            selectedYear);
                    edtDate.setText(formattedDate);
                    tilDate.setError(null);
                },
                year,
                month,
                day);

        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, selectedHour, selectedMinute) -> {
                    String amPm = selectedHour >= 12 ? "PM" : "AM";
                    int hour12 = selectedHour % 12;
                    if (hour12 == 0) hour12 = 12;
                    String formattedTime = String.format(
                            Locale.ROOT, "%02d:%02d %s", hour12, selectedMinute, amPm);
                    edtTime.setText(formattedTime);
                    tilTime.setError(null);
                },
                hour,
                minute,
                false);
        timePickerDialog.show();
    }

    private void populateEditModeFields(@Nullable MeetingDetail detail) {
        if (detail == null) return;

        if (detail.title() != null) {
            edtMeetingTopic.setText(detail.title());
        }

        if (detail.startTime() != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ROOT);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ROOT);

            edtDate.setText(detail.startTime().format(dateFormatter));
            edtTime.setText(detail.startTime().format(timeFormatter));

            if (detail.endTime() != null) {
                long durationMinutes = java.time.Duration.between(
                        detail.startTime(), detail.endTime()).toMinutes();
                String durationText = formatDurationForDisplay(durationMinutes);
                tvDuration.setText(durationText, false);
            }
        }

        MeetingSettings settings = detail.settings();
        if (settings != null) {
            switchWaitingRoom.setChecked(settings.isWaitingRoomEnabled());
            switchAllowGuest.setChecked(settings.isAllowGuest());
            switchPassword.setChecked(settings.hasPassword());
            tilPassword.setVisibility(settings.hasPassword() ? View.VISIBLE : View.GONE);
            edtMaxParticipants.setText(String.valueOf(settings.getMaxParticipants()));
            switchAllowScreenShare.setChecked(settings.isAllowScreenShare());
            switchChatEnabled.setChecked(settings.isChatEnabled());
            switchAllowMicrophone.setChecked(settings.isAllowMicrophone());
            switchAllowVideo.setChecked(settings.isAllowVideo());
        }
    }

    private String formatDurationForDisplay(long minutes) {
        String[] durations = getResources().getStringArray(R.array.schedule_durations);
        int[] durationMinutes = {30, 45, 60, 90, 120};

        if (minutes <= 0) {
            return durations.length > 0 ? durations[0] : "30 minutes";
        }

        for (int i = 0; i < durationMinutes.length; i++) {
            if (minutes == durationMinutes[i]) {
                return durations[i];
            }
        }

        if (minutes < 60) {
            return minutes + " minutes";
        } else if (minutes % 60 == 0) {
            int hours = (int) (minutes / 60);
            return hours == 1 ? "1 hour" : hours + " hours";
        } else {
            double hours = minutes / 60.0;
            return String.format(Locale.ROOT, "%.1f hours", hours);
        }
    }
}
