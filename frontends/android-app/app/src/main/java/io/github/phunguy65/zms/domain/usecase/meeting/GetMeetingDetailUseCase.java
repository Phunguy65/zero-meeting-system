package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.MeetingDetail;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for retrieving meeting details.
 *
 * <p>Used for pre-meeting edit mode and in-meeting settings display.
 * Returns meeting metadata along with current settings.
 */
public class GetMeetingDetailUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public GetMeetingDetailUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Retrieves full meeting details by ID.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes with meeting details
     */
    public CompletableFuture<MeetingDetail> execute(String meetingId) {
        return meetingRepository.getMeetingDetail(meetingId);
    }
}
