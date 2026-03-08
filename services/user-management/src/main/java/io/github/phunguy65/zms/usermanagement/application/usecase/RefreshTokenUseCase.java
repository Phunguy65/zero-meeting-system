package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.TokenProvider;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final UserPreferencesParser preferencesParser;
    private final long refreshTokenExpirySeconds;

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenProvider tokenProvider,
            RefreshTokenIssuer refreshTokenIssuer,
            UserPreferencesParser preferencesParser,
            @Value("${app.jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.preferencesParser = preferencesParser;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    @Transactional
    public Result<LoginResponse, AuthErrorCode> execute(RefreshTokenRequest request) {
        String tokenHash = refreshTokenIssuer.hash(request.refreshToken());

        var tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        RefreshToken token = tokenOpt.get();

        if (token.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(token.getUserId());
            return Result.failure(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }

        if (token.isExpired()) {
            return Result.failure(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        token.revoke();
        refreshTokenRepository.save(token);

        var userOpt = userRepository.findById(token.getUserId());
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.USER_NOT_FOUND);
        }
        var user = userOpt.get();

        String newAccessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getEmail().value());
        String newRawRefreshToken =
                refreshTokenIssuer.issueAndSave(user.getId(), refreshTokenExpirySeconds);

        return Result.success(new LoginResponse(
                newAccessToken,
                newRawRefreshToken,
                tokenProvider.getAccessTokenExpirySeconds(),
                preferencesParser.parseAsResponse(user.getPreferences())));
    }
}
