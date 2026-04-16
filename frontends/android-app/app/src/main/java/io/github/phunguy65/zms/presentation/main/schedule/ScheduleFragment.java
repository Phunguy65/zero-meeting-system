package io.github.phunguy65.zms.presentation.main.schedule;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.schedule.ScheduleViewModel;
import java.util.Calendar;
import java.util.Locale;

/**
 * Fragment for scheduling a new meeting.
 * Converted from ScheduleActivity to support single-activity navigation.
 */
@AndroidEntryPoint
public class ScheduleFragment extends Fragment {

    private ScheduleViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtMeetingTopic, edtDate, edtTime;
    private AutoCompleteTextView tvDuration;
    private MaterialSwitch switchWaitingRoom, switchHostVideo;
    private MaterialButton btnScheduleMeeting;

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
        setupDropdown();
        setupListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        edtMeetingTopic = view.findViewById(R.id.edtMeetingTopic);
        edtDate = view.findViewById(R.id.edtDate);
        edtTime = view.findViewById(R.id.edtTime);
        tvDuration = view.findViewById(R.id.tvDuration);
        switchWaitingRoom = view.findViewById(R.id.switchWaitingRoom);
        switchHostVideo = view.findViewById(R.id.switchHostVideo);
        btnScheduleMeeting = view.findViewById(R.id.btnScheduleMeeting);
    }

    private void setupDropdown() {
        String[] durations = getResources().getStringArray(R.array.schedule_durations);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, durations);
        tvDuration.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        edtDate.setOnClickListener(v -> showDatePicker());
        edtTime.setOnClickListener(v -> showTimePicker());

        btnScheduleMeeting.setOnClickListener(v -> {
            String topic = edtMeetingTopic.getText() != null
                    ? edtMeetingTopic.getText().toString().trim()
                    : "";
            String date =
                    edtDate.getText() != null ? edtDate.getText().toString().trim() : "";
            String time =
                    edtTime.getText() != null ? edtTime.getText().toString().trim() : "";
            String duration = tvDuration.getText() != null
                    ? tvDuration.getText().toString().trim()
                    : "";
            boolean isWaitingRoom = switchWaitingRoom.isChecked();
            boolean isHostVideoOn = switchHostVideo.isChecked();

            if (topic.isEmpty()) {
                Snackbar.make(v, R.string.schedule_error_empty_topic, Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }

            viewModel.scheduleMeeting(topic, date, time, duration, isWaitingRoom, isHostVideoOn);
            Snackbar.make(v, R.string.schedule_success, Snackbar.LENGTH_SHORT).show();
            Navigation.findNavController(v).popBackStack();
        });
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
                },
                year,
                month,
                day);
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
                },
                hour,
                minute,
                false);
        timePickerDialog.show();
    }
}
