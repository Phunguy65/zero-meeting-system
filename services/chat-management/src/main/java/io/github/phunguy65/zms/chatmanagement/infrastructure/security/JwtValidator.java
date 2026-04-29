package io.github.phunguy65.zms.chatmanagement.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates JWT tokens and extracts the userId (subject claim) for chat endpoints.
 *
 * <p>Uses the same secret as meeting-management (shared via environment variable).
 */
@Component
public class JwtValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    private final SecretKey secretKey;

    public JwtValidator(@Value("${app.chat.jwt-secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validates a JWT token and returns the user ID (subject claim).
     *
     * @param token the raw JWT string
     * @return the userId, or {@code null} if the token is invalid
     */
    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Returns {@code true} if the given token is valid (not expired, properly signed).
     */
    public boolean isValid(String token) {
        return extractUserId(token) != null;
    }
}
