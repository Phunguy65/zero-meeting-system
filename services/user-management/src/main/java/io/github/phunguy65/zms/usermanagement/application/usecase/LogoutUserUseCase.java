package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUserUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutUserUseCase(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public Result<Void, AuthErrorCode> execute(LogoutRequest request) {
        String tokenHash = LoginUserUseCase.sha256Hex(request.refreshToken());

        var tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        var token = tokenOpt.get();

        if (!token.isRevoked()) {
            token.revoke();
            refreshTokenRepository.save(token);
        }

        return Result.success();
    }
}
