package io.github.phunguy65.zms.usermanagement.domain.port;

import org.jspecify.annotations.Nullable;

/**
 * Outbound port: rate limiting for password reset requests.
 */
public interface PasswordResetRateLimiter {

    /**
     * Checks if a password reset request is allowed based on rate limits.
     *
     * @param email     the email address requesting reset
     * @param ipAddress the client IP address (may be null)
     * @return {@code true} if the request is allowed
     */
    boolean isAllowed(String email, @Nullable String ipAddress);

    /**
     * Records a password reset attempt for rate limiting purposes.
     *
     * @param email     the email address
     * @param ipAddress the client IP address (may be null)
     */
    void recordAttempt(String email, @Nullable String ipAddress);
}
