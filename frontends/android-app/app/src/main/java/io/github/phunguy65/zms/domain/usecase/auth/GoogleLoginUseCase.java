package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for Google Sign-In via Firebase ID token. */
public class GoogleLoginUseCase {

    private final AuthRepository authRepository;

    @Inject
    public GoogleLoginUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Executes the Google login operation asynchronously.
     *
     * @param idToken Firebase ID token obtained from Firebase Auth SDK
     * @return a future that completes with login result containing access and refresh tokens
     */
    public CompletableFuture<LoginResult> execute(String idToken) {
        return authRepository.googleLogin(idToken);
    }
}
