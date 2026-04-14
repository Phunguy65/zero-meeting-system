package com.example.zeromeeting.view.schedule; // Đổi lại package theo ý bạn

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.core.model.meeting.MeetingSettings;
import com.example.zeromeeting.core.model.meeting.ScheduleMeetingRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class ScheduleActivity extends AppCompatActivity {

    private ScheduleViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtMeetingTopic, edtDate, edtTime;
    private AutoCompleteTextView tvDuration;
    private MaterialSwitch switchWaitingRoom, switchHostVideo;
    private MaterialButton btnScheduleMeeting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);

        initViews();
        setupDropdown();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtMeetingTopic = findViewById(R.id.edtMeetingTopic);
        edtDate = findViewById(R.id.edtDate);
        edtTime = findViewById(R.id.edtTime);
        tvDuration = findViewById(R.id.tvDuration);
        switchWaitingRoom = findViewById(R.id.switchWaitingRoom);
        switchHostVideo = findViewById(R.id.switchHostVideo);
        btnScheduleMeeting = findViewById(R.id.btnScheduleMeeting);
    }

    private void setupDropdown() {
        // Thiết lập dữ liệu cho menu xổ xuống của Duration
        String[] durations = new String[]{"30 minutes", "45 minutes", "1 hour", "1.5 hours", "2 hours"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, durations);
        tvDuration.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Hiện khung chọn Ngày
        edtDate.setOnClickListener(v -> showDatePicker());

        // Hiện khung chọn Giờ
        edtTime.setOnClickListener(v -> showTimePicker());

        btnScheduleMeeting.setOnClickListener(v -> {
            String topic = edtMeetingTopic.getText().toString().trim();
            String date = edtDate.getText().toString().trim();
            String time = edtTime.getText().toString().trim();
            String duration = tvDuration.getText().toString().trim();
            boolean isWaitingRoom = switchWaitingRoom.isChecked();
            boolean isHostVideoOn = switchHostVideo.isChecked();

            if (topic.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập chủ đề cuộc họp", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- BƯỚC 1: GOM DỮ LIỆU TỪ UI THÀNH ĐỐI TƯỢNG REQUEST ---

            // 1.1 Cấu hình Settings cho cuộc họp
            MeetingSettings settings = new MeetingSettings();
            // API yêu cầu admissionPolicy là ALLOW_ALL hoặc MANUAL_APPROVAL
            settings.setAdmissionPolicy(isWaitingRoom ? "MANUAL_APPROVAL" : "ALLOW_ALL");
            settings.setMuteOnEntry(true); // Ví dụ mặc định
            settings.setChatEnabled(true);
            // (Lưu ý: API của bạn không có trường isHostVideoOn trong MeetingSettings,
            // nên ta tạm bỏ qua hoặc bạn có thể báo Backend bổ sung sau)

            // 1.2 Tạo Request chính
            ScheduleMeetingRequest request = new ScheduleMeetingRequest();
            request.setTitle(topic);

            // Lưu ý: Backend yêu cầu thời gian chuẩn ISO-8601 (VD: 2026-04-14T08:51:54Z)
            // Để đơn giản lúc này, mình ghép chuỗi tạm thời.
            // (Trong thực tế bạn nên dùng SimpleDateFormat để parse date/time chuẩn nhé)
            String startTimeISO = date + "T" + time + ":00Z";
            request.setStartTime(startTimeISO);

            // Tạm thời gán endTime giống startTime (bạn có thể cộng thêm duration sau)
            request.setEndTime(startTimeISO);
            request.setSettings(settings);

            // --- BƯỚC 2: GỌI VIEWMODEL VÀ LẮNG NGHE KẾT QUẢ (OBSERVE) ---

            // Gọi đúng tên hàm scheduleNewMeeting mà chúng ta đã viết
            viewModel.scheduleNewMeeting(request).observe(this, resource -> {
                switch (resource.status) {
                    case LOADING:
                        // Hiện thanh quay quay (ProgressBar) ở đây, khóa nút bấm lại để user không bấm 2 lần
                        btnScheduleMeeting.setEnabled(false);
                        btnScheduleMeeting.setText("Đang tạo...");
                        break;

                    case SUCCESS:
                        // Thành công thật sự từ Server trả về!
                        Toast.makeText(this, "Tạo lịch họp thành công!", Toast.LENGTH_SHORT).show();
                        finish(); // LÚC NÀY mới được đóng màn hình
                        break;

                    case ERROR:
                        // Lỗi mạng, hoặc lỗi từ Backend
                        btnScheduleMeeting.setEnabled(true);
                        btnScheduleMeeting.setText("Schedule Meeting");
                        Toast.makeText(this, "Lỗi: " + resource.message, Toast.LENGTH_LONG).show();
                        break;
                }
            });
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            // Định dạng ngày MM/DD/YYYY
            String formattedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", selectedMonth + 1, selectedDay, selectedYear);
            edtDate.setText(formattedDate);
        }, year, month, day);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (view, selectedHour, selectedMinute) -> {
            // Chuyển sang định dạng AM/PM
            String amPm = selectedHour >= 12 ? "PM" : "AM";
            int hour12 = selectedHour % 12;
            if (hour12 == 0) hour12 = 12;
            String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, selectedMinute, amPm);
            edtTime.setText(formattedTime);
        }, hour, minute, false);
        timePickerDialog.show();
    }
}
