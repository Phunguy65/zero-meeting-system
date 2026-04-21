package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import io.github.phunguy65.zms.domain.model.ScheduleMeetingRequest;
import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for meeting room operations.
 * Handles instant and scheduled meeting creation, settings management,
 * and upcoming meeting actions.
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

    /**
     * Retrieves upcoming host meetings for the dashboard.
     *
     * <p>Fetches meetings from the API, filters to SCHEDULED meetings with startTime in the future,
     * maps to UpcomingMeeting, and sorts by startTime ascending.
     *
     * @return a CompletableFuture that completes with the list of upcoming meetings
     */
    CompletableFuture<List<UpcomingMeeting>> getUpcomingHostMeetings();

    /**
     * Retrieves full meeting details by ID.
     *
     * <p>Used for pre-meeting edit mode and in-meeting settings display.
     * Returns meeting metadata along with current settings.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes with meeting details,
     *         or completes exceptionally if not found or unauthorized
     */
    CompletableFuture<MeetingDetail> getMeetingDetail(String meetingId);

    /**
     * Updates meeting settings for a SCHEDULED or LIVE meeting.
     *
     * <p>Replaces the meeting settings via PUT /api/v1/meetings/{id}/settings.
     * Supports host-only live meeting settings and pre-meeting settings edits.
     *
     * @param meetingId the meeting UUID
     * @param settings the new meeting settings to apply
     * @return a CompletableFuture that completes with the updated settings from server response,
     *         or completes exceptionally with a localized error message
     */
    CompletableFuture<MeetingSettings> updateMeetingSettings(String meetingId, MeetingSettings settings);

    /**
     * Cancels a scheduled meeting.
     *
     * <p>Used by the upcoming meeting card options menu.
     * Only SCHEDULED meetings can be cancelled.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes when cancellation succeeds,
     *         or completes exceptionally with a localized error message
     */
    CompletableFuture<Void> cancelMeeting(String meetingId);

    /**
     * Retrieves meeting details by short code.
     *
     * <p>Used for pre-join lookup to determine if the meeting requires a password
     * before attempting to join. Returns meeting metadata along with current settings.
     *
     * @param shortCode the meeting short code
     * @return a CompletableFuture that completes with meeting details,
     *         or completes exceptionally if not found or network error
     */
    CompletableFuture<MeetingDetail> getMeetingByShortCode(String shortCode);
}
