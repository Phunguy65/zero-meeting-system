package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class DeleteAccountUseCaseTest {

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    DeleteAccountUseCase useCase;

    private UUID userId;
    private User activeUser;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.reconstitute(
                userId,
                Email.of("alice@example.com"),
                HashedPassword.of("$argon2id$hash"),
                FullName.of("Alice"),
                null,
                null,
                Instant.now(),
                Instant.now(),
                null);
        deletedUser = User.reconstitute(
                userId,
                Email.of("alice@example.com"),
                HashedPassword.of("$argon2id$hash"),
                FullName.of("Alice"),
                null,
                null,
                Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(50),
                Instant.now().minusSeconds(50));
    }

    @Test
    void successPath_setsDeletedAtAndRevokesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(userId);

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(activeUser.isDeleted()).isTrue();
        assertThat(activeUser.getDeletedAt()).isNotNull();
        verify(userRepository).save(activeUser);
        verify(refreshTokenRepository).revokeAllByUserId(userId);
    }

    @Test
    void userNotFound_returnsUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        var result = useCase.execute(userId);

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void tokenRevocationIsCalled() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(userId);

        verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
    }
}
