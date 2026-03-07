package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import java.util.Optional;
import java.util.UUID;

/** Outbound port: persistence operations for the {@link User} aggregate. */
public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    User save(User user);

    boolean existsByEmail(Email email);
}
