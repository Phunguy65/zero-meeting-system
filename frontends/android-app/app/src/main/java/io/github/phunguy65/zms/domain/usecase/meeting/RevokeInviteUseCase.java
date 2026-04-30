package io.github.phunguy65.zms.domain.usecase.meeting;

import io.github.phunguy65.zms.domain.model.InviteeInfo;
import io.github.phunguy65.zms.domain.repository.MeetingRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for revoking an invitee's pending invite token.
 *
 * <p>Host-only operation. Sets the invitee's token status to REVOKED so the
 * existing invite link can no longer be used to join the meeting.
 */
public class RevokeInviteUseCase {

    private final MeetingRepository meetingRepository;

    @Inject
    public RevokeInviteUseCase(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    /**
     * Revokes an invitee and their pending invite token.
     *
     * @param meetingId the meeting UUID
     * @param inviteeId the invitee UUID
     * @return a CompletableFuture that completes with the updated invitee info
     */
    public CompletableFuture<InviteeInfo> execute(String meetingId, String inviteeId) {
        return meetingRepository.revokeInvite(meetingId, inviteeId);
    }
}
