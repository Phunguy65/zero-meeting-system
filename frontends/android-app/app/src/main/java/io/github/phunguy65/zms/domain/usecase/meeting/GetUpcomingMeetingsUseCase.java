package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.UpcomingMeeting;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for retrieving upcoming host meetings for the dashboard.
 *
 * <p>Returns scheduled meetings with startTime in the future, sorted by startTime ascending.
 */
public class GetUpcomingMeetingsUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public GetUpcomingMeetingsUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Retrieves upcoming host meetings for the dashboard.
     *
     * @return a CompletableFuture that completes with the list of upcoming meetings
     */
    public CompletableFuture<List<UpcomingMeeting>> execute() {
        return meetingRepository.getUpcomingHostMeetings();
    }
}
