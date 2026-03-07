package io.github.phunguy65.zms.usermanagement.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars-long!!";
    private static final long EXPIRY = 900L;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, EXPIRY);
    }

    @Test
    void generateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "alice@example.com");

        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void extractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "alice@example.com");

        assertThat(provider.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractEmailFromToken() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateAccessToken(userId, "alice@example.com");

        assertThat(provider.extractEmail(token)).isEqualTo("alice@example.com");
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(provider.validateToken("not.a.valid.token")).isFalse();
        assertThat(provider.validateToken("")).isFalse();
    }

    @Test
    void expiredTokenReturnsFalse() throws InterruptedException {
        // Create provider with 1-second expiry
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, 1L);
        String token = shortLived.generateAccessToken(UUID.randomUUID(), "test@example.com");

        // Wait for expiry
        Thread.sleep(1500);

        assertThat(shortLived.validateToken(token)).isFalse();
    }
}
