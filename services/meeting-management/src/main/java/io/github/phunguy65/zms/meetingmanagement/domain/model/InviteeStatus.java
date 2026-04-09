package io.github.phunguy65.zms.meetingmanagement.domain.model;

/**
 * Status of a meeting invitation.
 */
public enum InviteeStatus {
    PENDING,
    ACCEPTED,
    DECLINED;

    public boolean canTransitionTo(InviteeStatus target) {
        return switch (this) {
            case PENDING -> target == ACCEPTED || target == DECLINED;
            case ACCEPTED -> target == DECLINED;
            case DECLINED -> false;
        };
    }
}
