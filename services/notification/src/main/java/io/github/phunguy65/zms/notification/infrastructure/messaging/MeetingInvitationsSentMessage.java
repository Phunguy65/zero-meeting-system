package io.github.phunguy65.zms.notification.infrastructure.messaging;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MeetingInvitationsSentMessage(
        UUID eventId,
        UUID aggregateId,
        @Nullable String meetingTitle,
        String meetingShortCode,
        @Nullable Instant startTime,
        @Nullable String rawPassword,
        List<InviteeInfo> invitees,
        Instant occurredAt) {

    public record InviteeInfo(
            @Nullable UUID userId, String email, @Nullable String displayName) {}
}
