package io.github.phunguy65.zms.presentation.schedule;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.MeetingSettingsInput;
import io.github.phunguy65.zms.domain.model.ScheduleMeetingRequest;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.meeting.ScheduleMeetingUseCase;
import io.github.phunguy65.zms.presentation.common.util.SingleLiveEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * ViewModel for scheduling meetings.
 * Handles form validation, date/time parsing, settings management, and API submission.
 */
@HiltViewModel
public class ScheduleViewModel extends ViewModel {

    public static final int TITLE_MAX_LENGTH = 255;
    public static final int DURATION_MIN_MINUTES = 15;
    public static final int DURATION_MAX_MINUTES = 480;
    public static final int MAX_PARTICIPANTS_MIN = 2;
    public static final int MAX_PARTICIPANTS_MAX = 1000;
    public static final int MAX_PARTICIPANTS_DEFAULT = 100;

    private final ScheduleMeetingUseCase scheduleMeetingUseCase;
    private final SessionRepository sessionRepository;
    private final Executor mainExecutor;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final SingleLiveEvent<MeetingCreationResult> _scheduleSuccess = new SingleLiveEvent<>();
    public LiveData<MeetingCreationResult> scheduleSuccess = _scheduleSuccess;

    private final SingleLiveEvent<String> _scheduleError = new SingleLiveEvent<>();
    public LiveData<String> scheduleError = _scheduleError;

    private final SingleLiveEvent<String> _validationError = new SingleLiveEvent<>();
    public LiveData<String> validationError = _validationError;

    private final MutableLiveData<Boolean> _allowGuest = new MutableLiveData<>(true);
    public LiveData<Boolean> allowGuest = _allowGuest;

    private final MutableLiveData<Boolean> _passwordEnabled = new MutableLiveData<>(false);
    public LiveData<Boolean> passwordEnabled = _passwordEnabled;

    private final MutableLiveData<Integer> _maxParticipants =
            new MutableLiveData<>(MAX_PARTICIPANTS_DEFAULT);
    public LiveData<Integer> maxParticipants = _maxParticipants;

    private final MutableLiveData<Boolean> _allowScreenShare = new MutableLiveData<>(true);
    public LiveData<Boolean> allowScreenShare = _allowScreenShare;

    private final MutableLiveData<Boolean> _chatEnabled = new MutableLiveData<>(true);
    public LiveData<Boolean> chatEnabled = _chatEnabled;

    private final MutableLiveData<Boolean> _allowMicrophone = new MutableLiveData<>(true);
    public LiveData<Boolean> allowMicrophone = _allowMicrophone;

    private final MutableLiveData<Boolean> _allowVideo = new MutableLiveData<>(true);
    public LiveData<Boolean> allowVideo = _allowVideo;

    private final MutableLiveData<String> _endTimeText = new MutableLiveData<>(null);
    public LiveData<String> endTimeText = _endTimeText;

    @Inject
    public ScheduleViewModel(
            ScheduleMeetingUseCase scheduleMeetingUseCase,
            SessionRepository sessionRepository,
            @MainExecutor Executor mainExecutor) {
        this.scheduleMeetingUseCase = scheduleMeetingUseCase;
        this.sessionRepository = sessionRepository;
        this.mainExecutor = mainExecutor;
    }

    public void setAllowGuest(boolean enabled) {
        _allowGuest.setValue(enabled);
    }

    public void setPasswordEnabled(boolean enabled) {
        _passwordEnabled.setValue(enabled);
    }

    public void setMaxParticipants(int max) {
        _maxParticipants.setValue(max);
    }

    public void setAllowScreenShare(boolean enabled) {
        _allowScreenShare.setValue(enabled);
    }

    public void setChatEnabled(boolean enabled) {
        _chatEnabled.setValue(enabled);
    }

    public void setAllowMicrophone(boolean enabled) {
        _allowMicrophone.setValue(enabled);
    }

    public void setAllowVideo(boolean enabled) {
        _allowVideo.setValue(enabled);
    }

    /**
     * Updates the derived end time helper text based on current scheduling inputs.
     * Call this whenever date, time, or duration changes.
     *
     * @param date the selected date string
     * @param time the selected time string
     * @param duration the selected duration string
     */
    public void updateEndTime(
            @Nullable String date, @Nullable String time, @Nullable String duration) {
        if (date == null
                || date.trim().isEmpty()
                || time == null
                || time.trim().isEmpty()
                || duration == null
                || duration.trim().isEmpty()) {
            _endTimeText.setValue(null);
            return;
        }

        try {
            OffsetDateTime startTime = parseDateTime(date, time);
            int durationMinutes = parseDuration(duration);

            if (durationMinutes < DURATION_MIN_MINUTES || durationMinutes > DURATION_MAX_MINUTES) {
                _endTimeText.setValue(null);
                return;
            }

            OffsetDateTime endTime = startTime.plusMinutes(durationMinutes);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ROOT);
            _endTimeText.setValue(endTime.format(formatter));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            _endTimeText.setValue(null);
        }
    }

    /**
     * Validation result for inline field validation on blur.
     */
    public static class ValidationResult {
        public final boolean isValid;

        @Nullable public final String errorCode;

        private ValidationResult(boolean isValid, @Nullable String errorCode) {
            this.isValid = isValid;
            this.errorCode = errorCode;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorCode) {
            return new ValidationResult(false, errorCode);
        }
    }

    /**
     * Validates the title field for inline blur validation.
     * Title is optional but has a 255-character maximum.
     */
    public ValidationResult validateTitle(@Nullable String title) {
        if (title != null && title.length() > TITLE_MAX_LENGTH) {
            return ValidationResult.invalid("TITLE_TOO_LONG");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates the date field for inline blur validation.
     */
    public ValidationResult validateDate(@Nullable String date) {
        if (date == null || date.trim().isEmpty()) {
            return ValidationResult.invalid("EMPTY_DATE");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates the time field for inline blur validation.
     */
    public ValidationResult validateTime(@Nullable String time) {
        if (time == null || time.trim().isEmpty()) {
            return ValidationResult.invalid("EMPTY_TIME");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates the duration field for inline blur validation.
     */
    public ValidationResult validateDuration(@Nullable String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            return ValidationResult.invalid("EMPTY_DURATION");
        }
        try {
            int durationMinutes = parseDuration(duration);
            if (durationMinutes < DURATION_MIN_MINUTES || durationMinutes > DURATION_MAX_MINUTES) {
                return ValidationResult.invalid("INVALID_DURATION_RANGE");
            }
        } catch (IllegalArgumentException e) {
            return ValidationResult.invalid("INVALID_DURATION");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates the max participants field for inline blur validation.
     */
    public ValidationResult validateMaxParticipants(@Nullable String maxParticipantsStr) {
        if (maxParticipantsStr == null || maxParticipantsStr.trim().isEmpty()) {
            return ValidationResult.invalid("EMPTY_MAX_PARTICIPANTS");
        }
        try {
            int value = Integer.parseInt(maxParticipantsStr.trim());
            if (value < MAX_PARTICIPANTS_MIN || value > MAX_PARTICIPANTS_MAX) {
                return ValidationResult.invalid("INVALID_MAX_PARTICIPANTS_RANGE");
            }
        } catch (NumberFormatException e) {
            return ValidationResult.invalid("INVALID_MAX_PARTICIPANTS");
        }
        return ValidationResult.valid();
    }

    /**
     * Validates the password field for inline blur validation.
     * Password is only required when password protection is enabled.
     */
    public ValidationResult validatePassword(@Nullable String password) {
        Boolean passwordEnabled = _passwordEnabled.getValue();
        if (passwordEnabled != null && passwordEnabled) {
            if (password == null || password.trim().isEmpty()) {
                return ValidationResult.invalid("EMPTY_PASSWORD");
            }
        }
        return ValidationResult.valid();
    }

    /**
     * Schedules a meeting with the given form data.
     * Validates inputs before submission.
     */
    public void scheduleMeeting(
            @Nullable String topic,
            String date,
            String time,
            String duration,
            boolean isWaitingRoom,
            @Nullable String password) {

        if (Boolean.TRUE.equals(_isLoading.getValue())) {
            return;
        }

        // Title is optional but has a max length
        if (topic != null && topic.length() > TITLE_MAX_LENGTH) {
            _validationError.setValue("TITLE_TOO_LONG");
            return;
        }

        if (date == null || date.trim().isEmpty()) {
            _validationError.setValue("EMPTY_DATE");
            return;
        }

        if (time == null || time.trim().isEmpty()) {
            _validationError.setValue("EMPTY_TIME");
            return;
        }

        if (duration == null || duration.trim().isEmpty()) {
            _validationError.setValue("EMPTY_DURATION");
            return;
        }

        OffsetDateTime startTime;
        OffsetDateTime endTime;
        int durationMinutes;

        try {
            startTime = parseDateTime(date, time);
        } catch (DateTimeParseException e) {
            _validationError.setValue("INVALID_DATE_TIME");
            return;
        }

        if (startTime.isBefore(OffsetDateTime.now())) {
            _validationError.setValue("PAST_START_TIME");
            return;
        }

        try {
            durationMinutes = parseDuration(duration);
        } catch (IllegalArgumentException e) {
            _validationError.setValue("INVALID_DURATION");
            return;
        }

        if (durationMinutes < DURATION_MIN_MINUTES || durationMinutes > DURATION_MAX_MINUTES) {
            _validationError.setValue("INVALID_DURATION_RANGE");
            return;
        }

        Integer maxParticipantsVal = _maxParticipants.getValue();
        if (maxParticipantsVal == null
                || maxParticipantsVal < MAX_PARTICIPANTS_MIN
                || maxParticipantsVal > MAX_PARTICIPANTS_MAX) {
            _validationError.setValue("INVALID_MAX_PARTICIPANTS_RANGE");
            return;
        }

        endTime = startTime.plusMinutes(durationMinutes);

        _isLoading.setValue(true);

        // Build settings from current state using simplified contract
        Boolean passwordEnabledVal = _passwordEnabled.getValue();
        Boolean allowGuestVal = _allowGuest.getValue();
        Boolean allowScreenShareVal = _allowScreenShare.getValue();
        Boolean chatEnabledVal = _chatEnabled.getValue();
        Boolean allowMicrophoneVal = _allowMicrophone.getValue();
        Boolean allowVideoVal = _allowVideo.getValue();

        MeetingSettingsInput settings = new MeetingSettingsInput(
                isWaitingRoom,
                allowGuestVal != null ? allowGuestVal : true,
                (passwordEnabledVal != null
                                && passwordEnabledVal
                                && password != null
                                && !password.isEmpty())
                        ? password
                        : null,
                maxParticipantsVal,
                allowScreenShareVal != null ? allowScreenShareVal : true,
                chatEnabledVal != null ? chatEnabledVal : true,
                allowMicrophoneVal != null ? allowMicrophoneVal : true,
                allowVideoVal != null ? allowVideoVal : true);

        // Normalize topic: treat empty/whitespace-only as null
        String normalizedTopic = (topic == null || topic.trim().isEmpty()) ? null : topic.trim();

        ScheduleMeetingRequest request =
                new ScheduleMeetingRequest(normalizedTopic, startTime, endTime, settings);

        scheduleMeetingUseCase
                .execute(request)
                .whenCompleteAsync(
                        (result, error) -> {
                            _isLoading.setValue(false);

                            if (error != null) {
                                String errorMessage = error.getCause() != null
                                        ? error.getCause().getMessage()
                                        : error.getMessage();
                                _scheduleError.setValue(errorMessage);
                            } else {
                                _scheduleSuccess.setValue(result);
                            }
                        },
                        mainExecutor);
    }

    /**
     * Parses date and time strings into OffsetDateTime.
     * Expected formats: date "MM/dd/yyyy", time "hh:mm AM/PM"
     */
    private OffsetDateTime parseDateTime(String dateStr, String timeStr) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ROOT);
        LocalDate localDate = LocalDate.parse(dateStr, dateFormatter);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ROOT);
        LocalTime localTime = LocalTime.parse(timeStr.toUpperCase(Locale.ROOT), timeFormatter);

        ZoneId zoneId = ZoneId.systemDefault();
        return OffsetDateTime.of(
                localDate, localTime, zoneId.getRules().getOffset(localDate.atTime(localTime)));
    }

    /**
     * Parses duration string into minutes.
     * Uses numeric prefix extraction to support all locales (English: "30 minutes", Vietnamese: "30 phút").
     *
     * @throws IllegalArgumentException if the duration string is null, empty, or cannot be parsed
     */
    private int parseDuration(String durationStr) {
        if (durationStr == null || durationStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration is required");
        }

        String trimmed = durationStr.trim();

        StringBuilder numBuilder = new StringBuilder();
        for (char c : trimmed.toCharArray()) {
            if (Character.isDigit(c) || c == '.' || c == ',') {
                numBuilder.append(c);
            } else if (numBuilder.length() > 0) {
                break;
            }
        }

        if (numBuilder.length() == 0) {
            throw new IllegalArgumentException("Invalid duration format: no numeric value found");
        }

        try {
            String numStr = numBuilder.toString().replace(',', '.');
            double value = Double.parseDouble(numStr);

            if (value >= 10) {
                return (int) value;
            } else {
                return (int) (value * 60);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration format: " + durationStr, e);
        }
    }
}
