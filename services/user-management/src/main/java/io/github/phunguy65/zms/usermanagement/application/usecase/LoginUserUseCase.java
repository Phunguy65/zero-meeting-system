package io.github.phunguy65.zms.usermanagement.application.usecase;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.event.UserLoggedInEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.TokenProvider;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final UserPreferencesParser preferencesParser;
    private final long refreshTokenExpirySeconds;
    private final ApplicationEventPublisher eventPublisher;

    public LoginUserUseCase(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            TokenProvider tokenProvider,
            RefreshTokenIssuer refreshTokenIssuer,
            UserPreferencesParser preferencesParser,
            @Value("${app.jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.preferencesParser = preferencesParser;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
        this.eventPublisher = eventPublisher;
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

        // Guard: Google-only accounts have no password
        if (!user.hasPassword()) {
            return Result.failure(AuthErrorCode.INVALID_CREDENTIALS);
        }

        if (!passwordHasher.verify(request.password(), user.getHashedPassword().orElseThrow())) {
            return Result.failure(AuthErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getEmail().value());
        String rawRefreshToken =
                refreshTokenIssuer.issueAndSave(user.getId(), refreshTokenExpirySeconds);

        Instant loginAt = Instant.now();
        eventPublisher.publishEvent(new UserLoggedInEvent(
                UuidCreator.getTimeOrderedEpoch(), user.getId(), user.getEmail().value(), loginAt));

        return Result.success(new LoginResponse(
                accessToken,
                rawRefreshToken,
                tokenProvider.getAccessTokenExpirySeconds(),
                preferencesParser.parse(user.getPreferences())));
    }
}
