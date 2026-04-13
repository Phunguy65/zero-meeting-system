package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for requesting a password reset OTP. */
public class RequestPasswordResetUseCase {

    private final AuthRepository authRepository;

    @Inject
    public RequestPasswordResetUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Requests a password reset OTP to be sent to the given email.
     *
     * @param email user's email address
     * @return a future that completes on success
     */
    public CompletableFuture<Void> execute(String email) {
        return authRepository.forgotPassword(email);
    }
}
