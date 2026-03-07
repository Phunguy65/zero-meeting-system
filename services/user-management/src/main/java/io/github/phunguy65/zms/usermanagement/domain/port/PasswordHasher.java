package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;

/** Outbound port: password hashing and verification. */
public interface PasswordHasher {

    HashedPassword hash(String rawPassword);

    boolean verify(String rawPassword, HashedPassword hashedPassword);
}
