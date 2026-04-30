package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.InviteeInfo;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for resending a meeting invite to an existing invitee.
 *
 * <p>Host-only operation. Revokes the current invite token and issues a new one,
 * triggering a fresh invite email to the invitee.
 */
public class ResendInviteUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public ResendInviteUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Resends an invite to an existing invitee.
     *
     * @param meetingId the meeting UUID
     * @param inviteeId the invitee UUID
     * @return a CompletableFuture that completes with the updated invitee info
     */
    public CompletableFuture<InviteeInfo> execute(String meetingId, String inviteeId) {
        return meetingRepository.resendInvite(meetingId, inviteeId);
    }
}
