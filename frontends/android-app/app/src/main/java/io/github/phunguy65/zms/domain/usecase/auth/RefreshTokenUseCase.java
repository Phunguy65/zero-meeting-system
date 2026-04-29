package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/**
 * Use case for refreshing access tokens.
 *
 * <p>Used during auto-login to obtain fresh tokens when the user has a valid
 * refresh token and rememberMe is enabled.
 */
public class RefreshTokenUseCase {

    private final AuthRepository authRepository;

    @Inject
    public RefreshTokenUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Executes the token refresh operation asynchronously.
     *
     * @param refreshToken the current refresh token
     * @return a future that completes with new tokens on success
     */
    public CompletableFuture<LoginResult> execute(String refreshToken) {
        return authRepository.refreshToken(refreshToken);
    }
}
