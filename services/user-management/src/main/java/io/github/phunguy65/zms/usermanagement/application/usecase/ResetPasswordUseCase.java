package io.github.phunguy65.zms.usermanagement.application.usecase;

import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.usermanagement.application.command.ResetPasswordCommand;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.model.PasswordResetToken;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.port.OtpHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for resetting a password using an OTP.
 *
 * <p>This use case:
 * <ul>
 *   <li>Validates the OTP against the stored hash</li>
 *   <li>Checks token validity (not expired, not used, not locked)</li>
 *   <li>Updates the user's password</li>
 *   <li>Revokes all existing refresh tokens (security measure)</li>
 *   <li>Marks the token as used</li>
 * </ul>
 */
@Service
public class ResetPasswordUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final OtpHasher otpHasher;

    public ResetPasswordUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher,
            OtpHasher otpHasher) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.otpHasher = otpHasher;
    }

    /**
     * Resets the user's password using an OTP.
     *
     * @param command contains email, OTP, and new password
     * @return success or the appropriate error
     */
    @Transactional
    public Result<Void, AuthError> execute(ResetPasswordCommand command) {
        String email = command.email().toLowerCase().trim();
        var emailVo = Email.of(email);

        var userOpt = userRepository.findActiveByEmail(emailVo);
        if (userOpt.isEmpty()) {
            return Result.failure(new AuthError.OtpInvalid());
        }

        var user = userOpt.get();

        if (!user.hasPassword()) {
            return Result.failure(new AuthError.GoogleOnlyAccount());
        }

        var tokenOpt = tokenRepository.findValidByUserId(user.getId());
        if (tokenOpt.isEmpty()) {
            return Result.failure(new AuthError.OtpInvalid());
        }

        PasswordResetToken token = tokenOpt.get();

        if (token.isExpired()) {
            return Result.failure(new AuthError.OtpExpired());
        }

        if (token.isUsed()) {
            return Result.failure(new AuthError.OtpAlreadyUsed());
        }

        if (token.isLocked()) {
            return Result.failure(new AuthError.OtpLocked());
        }

        if (!otpHasher.verify(command.otp(), token.getOtpHash())) {
            token.incrementAttempts();
            tokenRepository.save(token);

            if (token.isLocked()) {
                return Result.failure(new AuthError.OtpLocked());
            }

            return Result.failure(new AuthError.OtpInvalid());
        }

        HashedPassword newHashedPassword = passwordHasher.hash(command.newPassword());
        user.updatePassword(newHashedPassword);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        token.markUsed();
        tokenRepository.save(token);

        return Result.success(null);
    }
}
