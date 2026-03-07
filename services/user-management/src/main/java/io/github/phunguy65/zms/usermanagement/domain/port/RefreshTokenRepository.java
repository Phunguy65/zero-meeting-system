package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.usermanagement.domain.model.RefreshToken;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: persistence operations for the {@link RefreshToken} aggregate. */
public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    RefreshToken save(RefreshToken refreshToken);

    void revokeAllByUserId(UUID userId);
}
