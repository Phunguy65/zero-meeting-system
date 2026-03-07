package io.github.phunguy65.zms.usermanagement.domain.event;

import io.github.phunguy65.zms.usermanagement.domain.PublishableEvent;
import java.time.Instant;
import java.util.UUID;

/** Published when a user successfully logs in. Topic: {@code user-management.user.logged-in}. */
public record UserLoggedInEvent(UUID eventId, UUID aggregateId, String email, Instant loginAt)
        implements PublishableEvent {

    @Override
    public String aggregateType() {
        return "user";
    }

    @Override
    public String eventType() {
        return "io.github.phunguy65.zms.user.logged-in.v1";
    }

    @Override
    public String topic() {
        return "user-management.user.logged-in";
    }

    @Override
    public Instant occurredAt() {
        return loginAt;
    }
}
