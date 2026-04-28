package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for ending a live meeting for all participants.
 *
 * <p>Host-only action. Only LIVE meetings can be ended via this use case.
 */
public class EndMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public EndMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Ends a live meeting for all participants.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes when the meeting is ended
     */
    public CompletableFuture<Void> execute(String meetingId) {
        return meetingRepository.endMeeting(meetingId);
    }
}
