package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import io.github.phunguy65.zms.usermanagement.infrastructure.messaging.KafkaEventPublisher;
import io.github.phunguy65.zms.usermanagement.infrastructure.messaging.OutboxEventPublisher;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test verifying that RegisterUserUseCase wires correctly with the Spring context.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegisterUserUseCaseIntegrationTest {

    @Autowired
    RegisterUserUseCase registerUserUseCase;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    // Mock Kafka infrastructure — no real broker needed in tests
    @MockitoBean
    KafkaEventPublisher kafkaEventPublisher;

    @MockitoBean
    OutboxEventPublisher outboxEventPublisher;

    @Test
    void successfulRegistration_contextLoadsAndUseCaseIsWired() {
        var request = new RegisterRequest(
                "integration-" + System.nanoTime() + "@example.com",
                "password123",
                "Integration User");

        var result = registerUserUseCase.execute(request);

        assertThat(result).isInstanceOf(Result.Success.class);
    }
}
