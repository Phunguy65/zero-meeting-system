package io.github.phunguy65.zms.domain.model;

import java.time.OffsetDateTime;
import org.jspecify.annotations.Nullable;

/**
 * Domain model representing a meeting invitee, including their invite token status.
 *
 * <p>Used by the host invite management screen to display invitee list with the
 * current state of each token so the host can resend or revoke as needed.
 */
public class InviteeInfo {

    private final String inviteeId;

    @Nullable private final String userId;

    @Nullable private final String email;

    @Nullable private final String displayName;

    @Nullable private final String status;

    @Nullable private final OffsetDateTime invitedAt;

    @Nullable private final OffsetDateTime respondedAt;

    @Nullable private final String tokenStatus;

    public InviteeInfo(
            String inviteeId,
            @Nullable String userId,
            @Nullable String email,
            @Nullable String displayName,
            @Nullable String status,
            @Nullable OffsetDateTime invitedAt,
            @Nullable OffsetDateTime respondedAt,
            @Nullable String tokenStatus) {
        this.inviteeId = inviteeId;
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.status = status;
        this.invitedAt = invitedAt;
        this.respondedAt = respondedAt;
        this.tokenStatus = tokenStatus;
    }

    public String getInviteeId() {
        return inviteeId;
    }

    public @Nullable String getUserId() {
        return userId;
    }

    public @Nullable String getEmail() {
        return email;
    }

    public @Nullable String getDisplayName() {
        return displayName;
    }

    public @Nullable String getStatus() {
        return status;
    }

    public @Nullable OffsetDateTime getInvitedAt() {
        return invitedAt;
    }

    public @Nullable OffsetDateTime getRespondedAt() {
        return respondedAt;
    }

    public @Nullable String getTokenStatus() {
        return tokenStatus;
    }
}
