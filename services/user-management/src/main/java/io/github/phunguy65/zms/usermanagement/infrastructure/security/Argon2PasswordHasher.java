package io.github.phunguy65.zms.usermanagement.infrastructure.security;

import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.port.PasswordHasher;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class Argon2PasswordHasher implements PasswordHasher {

    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 4, 65536, 3);

    @Override
    public HashedPassword hash(String rawPassword) {
        return HashedPassword.of(encoder.encode(rawPassword));
    }

    @Override
    public boolean verify(String rawPassword, HashedPassword hashedPassword) {
        return encoder.matches(rawPassword, hashedPassword.value());
    }
}
