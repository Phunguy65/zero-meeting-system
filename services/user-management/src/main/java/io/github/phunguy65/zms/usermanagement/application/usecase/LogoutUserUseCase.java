package io.github.phunguy65.zms.usermanagement.application.usecase;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.event.UserLoggedOutEvent;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUserUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final ApplicationEventPublisher eventPublisher;

    public LogoutUserUseCase(
            RefreshTokenRepository refreshTokenRepository,
            RefreshTokenIssuer refreshTokenIssuer,
            ApplicationEventPublisher eventPublisher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Result<Void, AuthErrorCode> execute(LogoutRequest request) {
        String tokenHash = refreshTokenIssuer.hash(request.refreshToken());

        var tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        var token = tokenOpt.get();

        if (!token.isRevoked()) {
            token.revoke();
            refreshTokenRepository.save(token);
        }

        eventPublisher.publishEvent(new UserLoggedOutEvent(
                UuidCreator.getTimeOrderedEpoch(), token.getUserId(), Instant.now()));

        return Result.success();
    }
}
