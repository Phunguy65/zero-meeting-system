package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Domain model representing a scheduled meeting request.
 * Encapsulates form data for scheduling a meeting.
 *
 * <p>Title is optional (nullable) to match the backend schema which accepts
 * an optional title with a 255-character maximum. Settings are encapsulated
 * in {@link MeetingSettingsInput} to carry all backend-supported options.
 */
public class ScheduleMeetingRequest {

    @Nullable private final String title;

    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final MeetingSettingsInput settings;

    public ScheduleMeetingRequest(
            @Nullable String title,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            MeetingSettingsInput settings) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.settings = settings;
    }

    @Nullable public String getTitle() {
        return title;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public MeetingSettingsInput getSettings() {
        return settings;
    }
}
