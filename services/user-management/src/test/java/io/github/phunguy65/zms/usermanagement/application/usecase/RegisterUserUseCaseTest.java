package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    RegisterUserUseCase useCase;

    @Test
    void successfulRegistration() {
        var request = new RegisterRequest("alice@example.com", "password123", "Alice Smith");
        when(userRepository.existsActiveByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("password123")).thenReturn(HashedPassword.of("$argon2id$hash"));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(request);

        assertThat(result).isInstanceOf(Result.Success.class);
        var success = (Result.Success<?, ?>) result;
        assertThat(success.value()).isNotNull();
        verify(passwordHasher).hash("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void duplicateEmailReturnsFailure() {
        var request = new RegisterRequest("alice@example.com", "password123", "Alice Smith");
        when(userRepository.existsActiveByEmail(Email.of("alice@example.com"))).thenReturn(true);

        var result = useCase.execute(request);

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void passwordHashingIsCalled() {
        var request = new RegisterRequest("bob@example.com", "securepass", "Bob");
        when(userRepository.existsActiveByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("securepass")).thenReturn(HashedPassword.of("$argon2id$xyz"));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(request);

        verify(passwordHasher, times(1)).hash("securepass");
    }
}
