package io.github.phunguy65.zms.usermanagement.infrastructure.persistence;

import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findActiveById(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findActiveByEmail(Email email) {
        return jpa.findByEmailAndDeletedAtIsNull(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findActiveByGoogleUid(String googleUid) {
        return jpa.findByGoogleUidAndDeletedAtIsNull(googleUid).map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public boolean existsActiveByEmail(Email email) {
        return jpa.existsByEmailAndDeletedAtIsNull(email.value());
    }

    private User toDomain(UserJpaEntity e) {
        String hash = e.getPasswordHash();
        return User.reconstitute(
                e.getId(),
                Email.of(e.getEmail()),
                hash != null ? HashedPassword.of(hash) : null,
                FullName.of(e.getFullName()),
                e.getAvatarUrl(),
                e.getGoogleUid(),
                e.getAuthProvider(),
                e.getPreferences(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getDeletedAt());
    }

    private UserJpaEntity toEntity(User u) {
        return new UserJpaEntity(
                u.getId(),
                u.getEmail().value(),
                u.getHashedPassword().map(HashedPassword::value).orElse(null),
                u.getFullName().value(),
                u.getAvatarUrl().orElse(null),
                u.getGoogleUid().orElse(null),
                u.getAuthProvider(),
                u.getPreferences().orElse(null),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getDeletedAt().orElse(null));
    }
}
