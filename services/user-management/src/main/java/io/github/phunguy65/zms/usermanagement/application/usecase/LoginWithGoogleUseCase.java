package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.usermanagement.application.dto.GoogleLoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginResponse;
import io.github.phunguy65.zms.usermanagement.application.service.RefreshTokenIssuer;
import io.github.phunguy65.zms.usermanagement.application.service.UserPreferencesParser;
import io.github.phunguy65.zms.usermanagement.domain.AuthErrorCode;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.GoogleAuthVerifier;
import io.github.phunguy65.zms.usermanagement.domain.port.TokenProvider;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginWithGoogleUseCase {

    private final GoogleAuthVerifier googleAuthVerifier;
    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final UserPreferencesParser preferencesParser;
    private final long refreshTokenExpirySeconds;

    public LoginWithGoogleUseCase(
            GoogleAuthVerifier googleAuthVerifier,
            UserRepository userRepository,
            TokenProvider tokenProvider,
            RefreshTokenIssuer refreshTokenIssuer,
            UserPreferencesParser preferencesParser,
            @Value("${app.jwt.refresh-token-expiry-seconds}") long refreshTokenExpirySeconds) {
        this.googleAuthVerifier = googleAuthVerifier;
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.preferencesParser = preferencesParser;
        this.refreshTokenExpirySeconds = refreshTokenExpirySeconds;
    }

    @Transactional
    public Result<LoginResponse, AuthErrorCode> execute(GoogleLoginRequest request) {
        var verifyResult = googleAuthVerifier.verify(request.idToken());
        if (verifyResult instanceof Result.Failure<?, AuthErrorCode> f) {
            return Result.failure(f.error());
        }
        var claims = ((Result.Success<
                                io.github.phunguy65.zms.usermanagement.domain.model
                                        .GoogleAuthClaims,
                                AuthErrorCode>)
                        verifyResult)
                .value();

        User user;
        var byGoogleUid = userRepository.findActiveByGoogleUid(claims.uid());
        if (byGoogleUid.isPresent()) {
            user = byGoogleUid.get();
        } else {
            var byEmail = userRepository.findByEmail(Email.of(claims.email()));
            if (byEmail.isPresent()) {
                var existing = byEmail.get();
                if (existing.isDeleted()) {
                    return Result.failure(AuthErrorCode.USER_DELETED);
                }
                existing.linkGoogle(claims.uid());
                user = userRepository.save(existing);
            } else {
                String displayName =
                        claims.displayName() != null ? claims.displayName() : claims.email();
                var newUser = User.registerWithGoogle(
                        Email.of(claims.email()),
                        claims.uid(),
                        FullName.of(displayName),
                        claims.photoUrl());
                user = userRepository.save(newUser);
            }
        }

        String accessToken =
                tokenProvider.generateAccessToken(user.getId(), user.getEmail().value());
        String rawRefreshToken =
                refreshTokenIssuer.issueAndSave(user.getId(), refreshTokenExpirySeconds);

        return Result.success(new LoginResponse(
                accessToken,
                rawRefreshToken,
                tokenProvider.getAccessTokenExpirySeconds(),
                preferencesParser.parse(user.getPreferences())));
    }
}
