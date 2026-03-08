package io.github.phunguy65.zms.usermanagement.domain.port;

import java.util.UUID;

public interface TokenProvider {

    String generateAccessToken(UUID userId, String email);

    long getAccessTokenExpirySeconds();

    boolean validateToken(String token);

    UUID extractUserId(String token);

    String extractEmail(String token);
}
