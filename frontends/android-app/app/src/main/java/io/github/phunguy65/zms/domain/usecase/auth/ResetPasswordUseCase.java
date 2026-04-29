package io.github.phunguy65.zms.domain.usecase.auth;

import io.github.phunguy65.zms.domain.repository.AuthRepository;
import java.util.concurrent.CompletableFuture;
import javax.inject.Inject;

/** Use case for resetting a password using an OTP. */
public class ResetPasswordUseCase {

    private final AuthRepository authRepository;

    @Inject
    public ResetPasswordUseCase(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    /**
     * Resets the user's password using an OTP.
     *
     * @param email user's email address
     * @param otp the 6-digit OTP received via email
     * @param newPassword the new password to set
     * @return a future that completes on success
     */
    public CompletableFuture<Void> execute(String email, String otp, String newPassword) {
        return authRepository.resetPassword(email, otp, newPassword);
    }
}
