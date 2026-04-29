package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for user login via email and password. */
public class LoginUseCase {

    private final AuthRepository authRepository;

    @Inject
    public LoginUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Executes the login operation asynchronously.
     *
     * @param email user's email address
     * @param password user's password
     * @return a future that completes with login result containing access and refresh tokens
     */
    public CompletableFuture<LoginResult> execute(String email, String password) {
        return authRepository.login(email, password);
    }
}
