package io.github.phunguy65.zms.usermanagement.domain.model;

import io.github.phunguy65.zms.shared.domain.ValueObject;

/**
 * Value object wrapping an Argon2id password hash string.
 */
public record HashedPassword(String value) implements ValueObject {

    public HashedPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("HashedPassword must not be blank");
        }
    }

    public static HashedPassword of(String hash) {
        return new HashedPassword(hash);
    }
}
