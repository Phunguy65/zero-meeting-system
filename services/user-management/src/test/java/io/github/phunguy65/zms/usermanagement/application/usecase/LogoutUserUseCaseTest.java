package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUserUseCaseTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    LogoutUserUseCase useCase;
    RefreshTokenIssuer refreshTokenIssuer;

    private static final String RAW_TOKEN = "someRawToken";

    @BeforeEach
    void setUp() {
        refreshTokenIssuer = new RefreshTokenIssuer(refreshTokenRepository);
        useCase = new LogoutUserUseCase(refreshTokenRepository, refreshTokenIssuer);
    }

    @Test
    void unknownTokenReturnsFailure() {
        String tokenHash = refreshTokenIssuer.hash(RAW_TOKEN);
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void alreadyRevokedIsIdempotentSuccess() {
        String tokenHash = refreshTokenIssuer.hash(RAW_TOKEN);
        var revoked = RefreshToken.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tokenHash,
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revoked));

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void successfulRevocation() {
        String tokenHash = refreshTokenIssuer.hash(RAW_TOKEN);
        var active = RefreshToken.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                tokenHash,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(active.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(active);
    }
}
