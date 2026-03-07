package io.github.phunguy65.zms.usermanagement.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserDomainEventsTest {

    @Test
    void userRegisteredEvent_hasCorrectFields() {
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        var event = new UserRegisteredEvent(eventId, userId, "alice@example.com", "Alice", now);

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.aggregateId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("alice@example.com");
        assertThat(event.fullName()).isEqualTo("Alice");
        assertThat(event.registeredAt()).isEqualTo(now);
        assertThat(event.aggregateType()).isEqualTo("user");
        assertThat(event.eventType()).isEqualTo("io.github.phunguy65.zms.user.registered.v1");
        assertThat(event.topic()).isEqualTo("user-management.user.registered");
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    void userLoggedInEvent_hasCorrectFields() {
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        var event = new UserLoggedInEvent(eventId, userId, "bob@example.com", now);

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.aggregateId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("bob@example.com");
        assertThat(event.loginAt()).isEqualTo(now);
        assertThat(event.aggregateType()).isEqualTo("user");
        assertThat(event.eventType()).isEqualTo("io.github.phunguy65.zms.user.logged-in.v1");
        assertThat(event.topic()).isEqualTo("user-management.user.logged-in");
    }

    @Test
    void userDeletedEvent_hasCorrectFields() {
        UUID eventId = UuidCreator.getTimeOrderedEpoch();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        var event = new UserDeletedEvent(eventId, userId, "carol@example.com", now);

        assertThat(event.eventId()).isEqualTo(eventId);
        assertThat(event.aggregateId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("carol@example.com");
        assertThat(event.deletedAt()).isEqualTo(now);
        assertThat(event.aggregateType()).isEqualTo("user");
        assertThat(event.eventType()).isEqualTo("io.github.phunguy65.zms.user.deleted.v1");
        assertThat(event.topic()).isEqualTo("user-management.user.deleted");
    }
}
