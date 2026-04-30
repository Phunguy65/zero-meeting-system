package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.InviteeInfo;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for retrieving the invitee list for a scheduled meeting.
 *
 * <p>Host-only operation. Returns invitees with their invite token status
 * so the host can decide whether to resend or revoke each invitee.
 */
public class GetInviteesUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public GetInviteesUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Retrieves all invitees for a meeting.
     *
     * @param meetingId the meeting UUID
     * @return a CompletableFuture that completes with the list of invitees
     */
    public CompletableFuture<List<InviteeInfo>> execute(String meetingId) {
        return meetingRepository.getInvitees(meetingId);
    }
}
