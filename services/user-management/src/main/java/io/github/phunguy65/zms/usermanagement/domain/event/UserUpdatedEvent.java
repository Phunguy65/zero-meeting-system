package io.github.phunguy65.zms.usermanagement.domain.event;

import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Published when a user's profile is updated via {@code PATCH /users/me}.
 * Carries enough data for downstream services to update local projections without
 * HTTP callbacks. Topic: {@code user-management.user.updated}.
 *
 * <p>Schema version: {@code io.github.phunguy65.zms.user.updated.v1}. Adding fields is safe;
 * removing or renaming fields is a breaking change.
 */
public record UserUpdatedEvent(
        UUID eventId,
        UUID aggregateId,
        String email,
        String fullName,
        @Nullable String avatarUrl,
        String authProvider,
        Instant updatedAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "user";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.user.updated.v1";
    }

    @Override
    public String topic() {
        return "user-management.user.updated";
    }

    @Override
    public Instant occurredAt() {
        return updatedAt;
    }
}
