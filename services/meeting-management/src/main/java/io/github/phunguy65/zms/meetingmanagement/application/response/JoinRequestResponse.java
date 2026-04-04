package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Response DTO for a join request in the pending queue.
 */
public record JoinRequestResponse(
        UUID id,
        UUID meetingId,
        @Nullable UUID userId,
        String displayName,
        JoinRequestStatus status,
        Instant requestedAt,
        Instant expiresAt) {}
