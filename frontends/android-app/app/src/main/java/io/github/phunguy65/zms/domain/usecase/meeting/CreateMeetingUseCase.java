package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import javax.inject.Inject;

/** Use case for creating a new meeting room. */
public class CreateMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public CreateMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }
}
