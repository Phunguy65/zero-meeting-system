package io.github.phunguy65.zms.usermanagement.domain.model;

import com.github.f4b6a3.uuid.UuidCreator;
import io.github.phunguy65.zms.shared.domain.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

/**
 * RefreshToken aggregate root. Represents a single issued refresh token.
 */
public class RefreshToken extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private Instant revokedAt;
    private final Instant createdAt;

    private RefreshToken(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.createdAt = createdAt;
    }

    /** Factory method for issuing a new refresh token. Generates a UUIDv7 primary key. */
    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        return new RefreshToken(
                UuidCreator.getTimeOrderedEpoch(),
                userId,
                tokenHash,
                expiresAt,
                null,
                Instant.now());
    }

    /** Reconstitution factory used by the persistence adapter. */
    public static RefreshToken reconstitute(
            UUID id,
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            Instant revokedAt,
            Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt, createdAt);
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }

    @Override
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
