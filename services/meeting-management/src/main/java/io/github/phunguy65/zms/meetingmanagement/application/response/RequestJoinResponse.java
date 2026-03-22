package io.github.phunguy65.zms.meetingmanagement.application.response;

import io.github.phunguy65.zms.meetingmanagement.domain.model.JoinRequestStatus;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * Response for a join request submission.
 *
 * <p>For {@code ALLOW_ALL} policy: returns {@code APPROVED} status with token immediately.
 * For {@code MANUAL_APPROVAL} policy: returns {@code PENDING} status with requestId for polling.
 */
public record RequestJoinResponse(
        UUID requestId,
        JoinRequestStatus status,
        @Nullable String token,
        @Nullable String roomName) {}
