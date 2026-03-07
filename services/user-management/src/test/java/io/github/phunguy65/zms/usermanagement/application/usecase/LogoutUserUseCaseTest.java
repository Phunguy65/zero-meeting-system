package io.github.phunguy65.zms.usermanagement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutUserUseCaseTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    LogoutUserUseCase useCase;

    private static final String RAW_TOKEN = "someRawToken";
    private static final String TOKEN_HASH = LoginUserUseCase.sha256Hex(RAW_TOKEN);

    @Test
    void unknownTokenReturnsFailure() {
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(((Result.Failure<?, AuthErrorCode>) result).error())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void alreadyRevokedIsIdempotentSuccess() {
        var revoked = RefreshToken.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TOKEN_HASH,
                Instant.now().plusSeconds(3600),
                Instant.now().minusSeconds(10),
                Instant.now().minusSeconds(100));
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(revoked));

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void successfulRevocation() {
        var active = RefreshToken.reconstitute(
                UUID.randomUUID(),
                UUID.randomUUID(),
                TOKEN_HASH,
                Instant.now().plusSeconds(3600),
                null,
                Instant.now().minusSeconds(10));
        when(refreshTokenRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(new LogoutRequest(RAW_TOKEN));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(active.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(active);
    }
}
