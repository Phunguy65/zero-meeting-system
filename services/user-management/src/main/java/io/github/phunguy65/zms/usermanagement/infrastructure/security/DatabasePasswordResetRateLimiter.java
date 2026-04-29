package io.github.phunguy65.zms.usermanagement.infrastructure.security;

import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.PasswordResetAttemptJpaEntity;
import io.github.phunguy65.zms.usermanagement.infrastructure.persistence.PasswordResetAttemptJpaRepository;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rate limiter for password reset requests.
 * Enforces per-email and per-IP limits to prevent abuse.
 */
@Component
public class DatabasePasswordResetRateLimiter
        implements io.github.phunguy65.zms.usermanagement.domain.port.PasswordResetRateLimiter {

    /** Maximum password reset requests per email per hour. */
    private static final int MAX_REQUESTS_PER_EMAIL_PER_HOUR = 5;

    /** Maximum password reset requests per IP per hour. */
    private static final int MAX_REQUESTS_PER_IP_PER_HOUR = 20;

    private static final Duration RATE_LIMIT_WINDOW = Duration.ofHours(1);

    private final PasswordResetAttemptJpaRepository attemptRepository;

    public DatabasePasswordResetRateLimiter(PasswordResetAttemptJpaRepository attemptRepository) {
        this.attemptRepository = attemptRepository;
    }

    @Override
    public boolean isAllowed(String email, @Nullable String ipAddress) {
        Instant windowStart = Instant.now().minus(RATE_LIMIT_WINDOW);

        long emailAttempts = attemptRepository.countByEmailAndCreatedAtAfter(email, windowStart);
        if (emailAttempts >= MAX_REQUESTS_PER_EMAIL_PER_HOUR) {
            return false;
        }

        if (ipAddress != null && !ipAddress.isBlank()) {
            long ipAttempts =
                    attemptRepository.countByIpAddressAndCreatedAtAfter(ipAddress, windowStart);
            if (ipAttempts >= MAX_REQUESTS_PER_IP_PER_HOUR) {
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional
    public void recordAttempt(String email, @Nullable String ipAddress) {
        PasswordResetAttemptJpaEntity attempt = new PasswordResetAttemptJpaEntity(email, ipAddress);
        attemptRepository.save(attempt);
    }
}
