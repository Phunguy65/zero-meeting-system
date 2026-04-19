package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.model.ScheduleMeetingRequest;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for scheduling a meeting.
 * Encapsulates the business logic for scheduled meeting creation.
 */
public class ScheduleMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public ScheduleMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Schedules a meeting with the given request data.
     *
     * @param request the schedule meeting request containing title, timing, and settings
     * @return a CompletableFuture that completes with the meeting creation result
     */
    public CompletableFuture<MeetingCreationResult> execute(ScheduleMeetingRequest request) {
        return meetingRepository.scheduleMeeting(request);
    }
}
