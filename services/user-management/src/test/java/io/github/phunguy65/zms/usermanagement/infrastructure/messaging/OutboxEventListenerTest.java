package io.github.phunguy65.zms.usermanagement.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.usermanagement.domain.event.UserRegisteredEvent;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.OutboxEventEntity;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutboxEventListenerTest {

    @Mock
    OutboxEventRepository outboxEventRepository;

    @Spy
    ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    OutboxEventListener listener;

    @Test
    void onPublishableEvent_persistsOutboxRow() {
        UUID userId = UUID.randomUUID();
        var event = new UserRegisteredEvent(
                UuidCreator.getTimeOrderedEpoch(),
                userId,
                "alice@example.com",
                "Alice",
                Instant.now());

        listener.onPublishableEvent(event);

        verify(outboxEventRepository).save(any(OutboxEventEntity.class));
    }
}
