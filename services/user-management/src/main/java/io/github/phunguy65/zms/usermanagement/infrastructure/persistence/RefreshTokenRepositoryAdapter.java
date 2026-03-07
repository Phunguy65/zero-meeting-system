package io.github.phunguy65.zms.usermanagement.infrastructure.persistence;

import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import io.github.phunguy65.zms.usermanagement.domain.port.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = toEntity(token);
        RefreshTokenJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpa.updateRevokedAtByUserId(userId, Instant.now());
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.reconstitute(
                e.getId(),
                e.getUserId(),
                e.getTokenHash(),
                e.getExpiresAt(),
                e.getRevokedAt(),
                e.getCreatedAt());
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken t) {
        return new RefreshTokenJpaEntity(
                t.getId(),
                t.getUserId(),
                t.getTokenHash(),
                t.getExpiresAt(),
                t.getRevokedAt(),
                t.getCreatedAt());
    }
}
