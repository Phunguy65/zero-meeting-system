package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.InstantMeetingSettings;
import io.github.phunguy65.zms.domain.model.MeetingCreationResult;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for creating an instant meeting.
 * Encapsulates the business logic for instant meeting creation.
 */
public class CreateMeetingUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public CreateMeetingUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Creates an instant meeting with the given settings.
     *
     * @param settings the instant meeting settings (waiting room, host video)
     * @return a CompletableFuture that completes with the meeting creation result
     */
    public CompletableFuture<MeetingCreationResult> execute(InstantMeetingSettings settings) {
        return meetingRepository.createInstantMeeting(settings);
    }
}
