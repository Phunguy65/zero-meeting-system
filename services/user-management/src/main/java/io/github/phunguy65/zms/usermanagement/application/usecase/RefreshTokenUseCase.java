package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.application.dto.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.infrastructure.security.JwtTokenProvider;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshTokenExpirySeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    @Transactional
    public Result<LoginResponse, AuthErrorCode> execute(RefreshTokenRequest request) {
        String tokenHash = LoginUserUseCase.sha256Hex(request.refreshToken());

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

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail().value());

        byte[] rawBytes = new byte[32];
        secureRandom.nextBytes(rawBytes);
        String newRawRefreshToken =
                Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);
        String newTokenHash = LoginUserUseCase.sha256Hex(newRawRefreshToken);

        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirySeconds);
        var newRefreshToken = RefreshToken.issue(user.getId(), newTokenHash, expiresAt);
        refreshTokenRepository.save(newRefreshToken);

        return Result.success(new LoginResponse(
                newAccessToken,
                newRawRefreshToken,
                jwtTokenProvider.getAccessTokenExpirySeconds()));
    }
}
