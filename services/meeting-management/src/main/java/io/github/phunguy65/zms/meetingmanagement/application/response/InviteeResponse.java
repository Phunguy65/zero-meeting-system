package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.InviteeStatus;
import io.github.phunguy65.zms.meetingmanagement.domain.projection.InviteeSummary;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record InviteeResponse(
        @Nullable UUID userId,
        String email,
        @Nullable String displayName,
        InviteeStatus status,
        Instant invitedAt,
        @Nullable Instant respondedAt) {

    public static InviteeResponse fromProjection(InviteeSummary projection) {
        return new InviteeResponse(
                projection.userId(),
                projection.email(),
                projection.displayName(),
                InviteeStatus.valueOf(projection.status()),
                projection.invitedAt(),
                projection.respondedAt());
    }
}
