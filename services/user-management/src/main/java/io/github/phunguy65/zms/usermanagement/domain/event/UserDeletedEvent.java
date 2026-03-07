package io.github.phunguy65.zms.usermanagement.domain.event;

import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a user account is soft-deleted. Topic: {@code user-management.user.deleted}.
 */
public record UserDeletedEvent(UUID eventId, UUID aggregateId, String email, Instant deletedAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "user";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.user.deleted.v1";
    }

    @Override
    public String topic() {
        return "user-management.user.deleted";
    }

    @Override
    public Instant occurredAt() {
        return deletedAt;
    }
}
