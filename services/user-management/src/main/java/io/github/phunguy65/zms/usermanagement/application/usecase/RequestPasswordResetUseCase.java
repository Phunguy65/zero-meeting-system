package io.github.phunguy65.zms.usermanagement.application.usecase;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.Result;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.usermanagement.application.command.RequestPasswordResetCommand;
import io.github.phunguy65.zms.usermanagement.domain.AuthError;
import io.github.phunguy65.zms.usermanagement.domain.event.PasswordResetRequestedEvent;
import io.github.phunguy65.zms.usermanagement.domain.model.PasswordResetToken;
import io.github.phunguy65.zms.usermanagement.domain.port.OtpGenerator;
import io.github.phunguy65.zms.usermanagement.domain.port.OtpHasher;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetRateLimiter;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetTokenRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for requesting a password reset OTP.
 *
 * <p>This use case:
 * <ul>
 *   <li>Checks rate limits (per-email and per-IP)</li>
 *   <li>Validates the email exists and has a password (not Google-only)</li>
 *   <li>Generates a 6-digit OTP and stores its hash</li>
 *   <li>Publishes a {@link PasswordResetRequestedEvent} for notification service</li>
 * </ul>
 *
 * <p>To prevent user enumeration, this returns success even if the email doesn't exist.
 * The notification service simply won't send an email in that case.
 */
@Service
public class RequestPasswordResetUseCase {

    /** OTP validity period: 15 minutes. */
    private static final Duration OTP_VALIDITY = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetRateLimiter rateLimiter;
    private final OtpGenerator otpGenerator;
    private final OtpHasher otpHasher;
    private final ApplicationEventPublisher eventPublisher;

    public RequestPasswordResetUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordResetRateLimiter rateLimiter,
            OtpGenerator otpGenerator,
            OtpHasher otpHasher,
            ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.rateLimiter = rateLimiter;
        this.otpGenerator = otpGenerator;
        this.otpHasher = otpHasher;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Requests a password reset for the given email.
     *
     * @param command contains email and IP address
     * @return success (always, to prevent enumeration) or rate limit error
     */
    @Transactional
    public Result<Void, AuthError> execute(RequestPasswordResetCommand command) {
        String email = command.email().toLowerCase().trim();

        // Check rate limit first (before any DB lookups to prevent timing attacks)
        if (!rateLimiter.isAllowed(email, command.ipAddress())) {
            return Result.failure(new AuthError.RateLimitExceeded());
        }

        // Record the attempt for rate limiting
        rateLimiter.recordAttempt(email, command.ipAddress());

        var emailVo = Email.of(email);
        var userOpt = userRepository.findActiveByEmail(emailVo);

        // If no user found, return success silently (prevent enumeration)
        if (userOpt.isEmpty()) {
            return Result.success(null);
        }

        var user = userOpt.get();

        // Check if user has a password (reject Google-only accounts)
        if (!user.hasPassword()) {
            // Still return success to prevent enumeration, but don't send OTP
            // The user will need to use Google Sign-In
            return Result.success(null);
        }

        // Invalidate any existing tokens for this user
        tokenRepository.invalidateAllByUserId(user.getId());

        // Generate OTP and create token
        String otp = otpGenerator.generate();
        String otpHash = otpHasher.hash(otp);
        Instant expiresAt = Instant.now().plus(OTP_VALIDITY);

        PasswordResetToken token = PasswordResetToken.issue(user.getId(), otpHash, expiresAt);
        tokenRepository.save(token);

        // Publish event for notification service (contains plaintext OTP for email)
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                UuidCreator.getTimeOrderedEpoch(),
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName().value(),
                otp,
                expiresAt,
                Instant.now()));

        return Result.success(null);
    }
}
