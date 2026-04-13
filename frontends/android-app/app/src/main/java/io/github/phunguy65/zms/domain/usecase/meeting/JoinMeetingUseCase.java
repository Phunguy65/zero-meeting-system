package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import javax.inject.Inject;

/** Use case for joining an existing meeting room. */
public class JoinMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public JoinMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }
}
