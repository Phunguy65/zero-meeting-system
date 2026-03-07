package io.github.phunguy65.zms.usermanagement.domain.event;

import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;

/**
 * Published when a new user successfully registers. Topic:
 * {@code user-management.user.registered}.
 */
public record UserRegisteredEvent(
        UUID eventId, UUID aggregateId, String email, String fullName, Instant registeredAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "user";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.user.registered.v1";
    }

    @Override
    public String topic() {
        return "user-management.user.registered";
    }

    @Override
    public Instant occurredAt() {
        return registeredAt;
    }
}
