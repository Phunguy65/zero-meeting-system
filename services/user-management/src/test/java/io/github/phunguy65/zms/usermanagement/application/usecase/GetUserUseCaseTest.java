package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.UserResponse;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class GetUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    GetUserUseCase useCase;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new GetUserUseCase(userRepository, new UserPreferencesParser(new ObjectMapper()));
    }

    private User buildUser() {
        return User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                null,
                FullName.of("Alice"),
                "https://example.com/avatar.png",
                null,
                "EMAIL",
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void execute_userFound_returnsUserResponse() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.of(buildUser()));

        var result = useCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (UserResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.id()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.fullName()).isEqualTo("Alice");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
        assertThat(response.authProvider()).isEqualTo("EMAIL");
        // Preferences are empty when null in DB
        assertThat(response.preferences().settings()).isEmpty();
    }

    @Test
    void execute_userNotFound_returnsFailure() {
        when(userRepository.findActiveById(USER_ID)).thenReturn(Optional.empty());

        var result = useCase.execute(USER_ID);

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
    }
}
