package io.github.phunguy65.zms.domain.model;

/**
 * Result of a meeting settings update operation.
 *
 * <p>Wraps the updated settings together with invite-token invalidation metadata.
 * When a password change revokes existing invite tokens, {@code resendInvitesRecommended}
 * is true and {@code invalidatedInviteCount} is non-zero.
 */
public class UpdateSettingsResult {

    private final MeetingSettings settings;
    private final int invalidatedInviteCount;
    private final boolean resendInvitesRecommended;

    public UpdateSettingsResult(
            MeetingSettings settings,
            int invalidatedInviteCount,
            boolean resendInvitesRecommended) {
        this.settings = settings;
        this.invalidatedInviteCount = invalidatedInviteCount;
        this.resendInvitesRecommended = resendInvitesRecommended;
    }

    /**
     * Factory method for the common case where no tokens were invalidated.
     */
    public static UpdateSettingsResult noInvalidation(MeetingSettings settings) {
        return new UpdateSettingsResult(settings, 0, false);
    }

    public MeetingSettings getSettings() {
        return settings;
    }

    public int getInvalidatedInviteCount() {
        return invalidatedInviteCount;
    }

    public boolean isResendInvitesRecommended() {
        return resendInvitesRecommended;
    }
}
