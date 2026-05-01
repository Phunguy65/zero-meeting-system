package io.github.phunguy65.zms.domain.model;

/**
 * Result of validating an invite token.
 *
 * <p>On success, contains the meeting ID and short code needed to proceed with the
 * join flow. {@code preApproved} indicates whether the token holder can bypass the
 * waiting room.
 */
public class InviteTokenValidationResult {

    private final String meetingId;
    private final String shortCode;
    private final boolean preApproved;

    public InviteTokenValidationResult(String meetingId, String shortCode, boolean preApproved) {
        this.meetingId = meetingId;
        this.shortCode = shortCode;
        this.preApproved = preApproved;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public String getShortCode() {
        return shortCode;
    }

    public boolean isPreApproved() {
        return preApproved;
    }
}
