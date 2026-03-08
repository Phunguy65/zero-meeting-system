package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: persistence operations for the {@link User} aggregate. */
public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    /** Returns the user only if {@code deleted_at IS NULL}. */
    Optional<User> findActiveById(UUID id);

    /** Returns the user only if {@code deleted_at IS NULL}. */
    Optional<User> findActiveByEmail(Email email);

    User save(User user);

    boolean existsByEmail(Email email);

    /** Returns {@code true} only if an active (non-deleted) user with this email exists. */
    boolean existsActiveByEmail(Email email);
}
