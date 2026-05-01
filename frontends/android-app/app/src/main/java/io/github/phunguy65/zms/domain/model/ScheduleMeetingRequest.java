package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Domain model representing a scheduled meeting request.
 * Encapsulates form data for scheduling a meeting.
 *
 * <p>Title is optional (nullable) to match the backend schema which accepts
 * an optional title with a 255-character maximum. Settings are encapsulated
 * in {@link MeetingSettingsInput} to carry all backend-supported options.
 *
 * <p>Invitees are only valid during meeting creation. The list is nullable and
 * may contain up to 10 email addresses enforced by the Android client.
 */
public class ScheduleMeetingRequest {

    @Nullable private final String title;

    private final OffsetDateTime startTime;
    private final OffsetDateTime endTime;
    private final MeetingSettingsInput settings;

    @Nullable private final List<String> invitees;

    public ScheduleMeetingRequest(
            @Nullable String title,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            MeetingSettingsInput settings,
            @Nullable List<String> invitees) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.settings = settings;
        this.invitees = invitees;
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

    @Nullable public List<String> getInvitees() {
        return invitees;
    }
}
