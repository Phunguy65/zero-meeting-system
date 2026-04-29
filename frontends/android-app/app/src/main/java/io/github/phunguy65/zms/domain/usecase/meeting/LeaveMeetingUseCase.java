package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import javax.inject.Inject;

/** Use case for leaving a meeting room and cleaning up resources. */
public class LeaveMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public LeaveMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }
}
