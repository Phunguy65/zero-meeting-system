package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordHasher passwordHasher;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    ApplicationEventPublisher eventPublisher;

    LoginUserUseCase useCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        useCase = new LoginUserUseCase(
                userRepository,
                passwordHasher,
                refreshTokenRepository,
                jwtTokenProvider,
                2592000L,
                eventPublisher);
        testUser = User.reconstitute(
                UUID.randomUUID(),
                Email.of("alice@example.com"),
                HashedPassword.of("$argon2id$hash"),
                FullName.of("Alice"),
                null,
                null,
                Instant.now(),
                Instant.now(),
                null);
    }

    @Test
    void successfulLogin() {
        when(userRepository.findByEmail(Email.of("alice@example.com")))
                .thenReturn(Optional.of(testUser));
        when(userRepository.findActiveByEmail(Email.of("alice@example.com")))
                .thenReturn(Optional.of(testUser));
        when(passwordHasher.verify("password123", testUser.getHashedPassword())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("access.token.here");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new LoginRequest("alice@example.com", "password123"));

        assertThat(result).isInstanceOf(Result.Success.class);
        var response = (LoginResponse) ((Result.Success<?, ?>) result).value();
        assertThat(response.accessToken()).isEqualTo("access.token.here");
        assertThat(response.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void wrongPasswordReturnsInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(userRepository.findActiveByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordHasher.verify(any(), any())).thenReturn(false);

        var result = useCase.execute(new LoginRequest("alice@example.com", "wrong"));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void unknownEmailReturnsInvalidCredentials() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.findActiveByEmail(any())).thenReturn(Optional.empty());

        var result = useCase.execute(new LoginRequest("nobody@example.com", "pass"));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void refreshTokenStoredAsHash() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(testUser));
        when(userRepository.findActiveByEmail(any())).thenReturn(Optional.of(testUser));
        when(passwordHasher.verify(any(), any())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("tok");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new LoginRequest("alice@example.com", "password123"));
        var response = (LoginResponse) ((Result.Success<?, ?>) result).value();

        // The raw token returned to client should NOT equal the stored hash
        String rawToken = response.refreshToken();
        String expectedHash = LoginUserUseCase.sha256Hex(rawToken);

        verify(refreshTokenRepository)
                .save(argThat((RefreshToken rt) -> rt.getTokenHash().equals(expectedHash)));
    }
}
