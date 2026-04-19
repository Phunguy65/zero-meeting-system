package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.ScheduleMeetingRequest;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for meeting room operations.
 * Handles instant and scheduled meeting creation.
 */
public interface MeetingRepository {

    /**
     * Creates an instant meeting with the given settings.
     *
     * @param settings the meeting settings including waiting room and host video preferences
     * @return a CompletableFuture that completes with the created meeting result,
     *         or completes exceptionally with a localized error message
     */
    CompletableFuture<MeetingCreationResult> createInstantMeeting(InstantMeetingSettings settings);

    /**
     * Schedules a meeting with the given request data.
     *
     * @param request the schedule meeting request containing title, timing, and settings
     * @return a CompletableFuture that completes with the created meeting result,
     *         or completes exceptionally with a localized error message
     */
    CompletableFuture<MeetingCreationResult> scheduleMeeting(ScheduleMeetingRequest request);
}
