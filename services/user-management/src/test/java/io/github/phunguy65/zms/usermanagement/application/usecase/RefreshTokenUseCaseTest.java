package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
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

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    JwtTokenProvider jwtTokenProvider;

    RefreshTokenUseCase useCase;

    private static final String RAW_TOKEN = "rawRefreshTokenValue";
    private static final String TOKEN_HASH = LoginUserUseCase.sha256Hex(RAW_TOKEN);
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(
                refreshTokenRepository, userRepository, jwtTokenProvider, 2592000L);
    }

    @Test
    void expiredTokenReturnsFailure() {
        var expired = RefreshToken.reconstitute(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                Instant.now().minusSeconds(1),
                null,
                Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(expired));

        var result = useCase.execute(new RefreshTokenRequest(RAW_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    @Test
    void revokedTokenTriggersReuseDetection() {
        var revoked = RefreshToken.reconstitute(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(revoked));

        var result = useCase.execute(new RefreshTokenRequest(RAW_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        verify(refreshTokenRepository).revokeAllByUserId(USER_ID);
    }

    @Test
    void validTokenRotation() {
        var valid = RefreshToken.reconstitute(
                UUID.randomUUID(),
                USER_ID,
                TOKEN_HASH,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now().minusSeconds(10));
        var user = User.reconstitute(
                USER_ID,
                Email.of("alice@example.com"),
                HashedPassword.of("$argon2id$h"),
                FullName.of("Alice"),
                null,
                null,
                Instant.now(),
                Instant.now(),
                null);

        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(valid));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any(), any())).thenReturn("new.access.token");
        when(jwtTokenProvider.getAccessTokenExpirySeconds()).thenReturn(900L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new RefreshTokenRequest(RAW_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        // Old token should be revoked
        assertThat(valid.isRevoked()).isTrue();
        // New token should be saved
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }
}
