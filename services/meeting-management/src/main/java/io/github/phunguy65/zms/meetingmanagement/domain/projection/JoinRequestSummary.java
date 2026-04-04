package io.github.phunguy65.zms.meetingmanagement.domain.projection;

import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record JoinRequestSummary(
        UUID id,
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        JoinRequestStatus status,
        Instant requestedAt,
        Instant expiresAt) {}
