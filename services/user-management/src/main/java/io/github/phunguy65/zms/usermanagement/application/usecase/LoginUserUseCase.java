package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.infrastructure.security.JwtTokenProvider;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final long refreshTokenExpirySeconds;

    private final SecureRandom secureRandom = new SecureRandom();

    public LoginUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    @Transactional
    public Result<LoginResponse, AuthErrorCode> execute(LoginRequest request) {
        var emailVo = Email.of(request.email());

        var anyUser = userRepository.findByEmail(emailVo);
        if (anyUser.isPresent() && anyUser.get().isDeleted()) {
            return Result.failure(AuthErrorCode.USER_DELETED);
        }

        var userOpt = userRepository.findActiveByEmail(emailVo);
        if (userOpt.isEmpty()) {
            return Result.failure(AuthErrorCode.INVALID_CREDENTIALS);
        }

        var user = userOpt.get();
        if (!passwordHasher.verify(request.password(), user.getHashedPassword())) {
            return Result.failure(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail().value());

        byte[] rawBytes = new byte[32];
        secureRandom.nextBytes(rawBytes);
        String rawRefreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

        String tokenHash = sha256Hex(rawRefreshToken);

        Instant expiresAt = Instant.now().plusSeconds(refreshTokenExpirySeconds);
        var refreshToken = RefreshToken.issue(user.getId(), tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return Result.success(new LoginResponse(
                accessToken, rawRefreshToken, jwtTokenProvider.getAccessTokenExpirySeconds()));
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
