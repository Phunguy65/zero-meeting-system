package io.github.phunguy65.zms.domain.repository;

import java.util.concurrent.CompletableFuture;

/**
 * Repository contract for managing meeting recording lifecycle.
 * Provides start and stop operations backed by the meeting-management recording API.
 */
public interface RecordingRepository {

    /**
     * Starts recording for a meeting.
     *
     * @param meetingId the UUID of the meeting to record
     * @return a future that completes when the recording has been started
     */
    CompletableFuture<Void> startRecording(String meetingId);

    /**
     * Stops recording for a meeting.
     *
     * @param meetingId the UUID of the meeting whose recording should stop
     * @return a future that completes when the stop request has been accepted
     */
    CompletableFuture<Void> stopRecording(String meetingId);
}
