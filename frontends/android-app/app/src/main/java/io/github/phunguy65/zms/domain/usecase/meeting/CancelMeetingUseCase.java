package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for cancelling a scheduled meeting.
 *
 * <p>Used by the upcoming meeting card options menu.
 * Only SCHEDULED meetings can be cancelled.
 */
public class CancelMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public CancelMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Cancels a scheduled meeting.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes when cancellation succeeds
     */
    public CompletableFuture<Void> execute(String meetingId) {
        return meetingRepository.cancelMeeting(meetingId);
    }
}
