package io.github.phunguy65.zms.usermanagement.infrastructure.persistence;

import io.github.phunguy65.zms.shared.domain.CursorPageResponse;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.shared.domain.valueobject.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.FullName;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.HashedPassword;
import io.github.phunguy65.zms.usermanagement.domain.model.valueobject.Username;
import io.github.phunguy65.zms.usermanagement.domain.port.UserRepository;
import io.github.phunguy65.zms.usermanagement.domain.port.UserScrollFilter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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

    @Override
    public boolean existsActiveByUsername(Username username) {
        return jpa.existsByUsernameAndDeletedAtIsNull(username.value());
    }

    @Override
    public Optional<User> findActiveByUsername(Username username) {
        return jpa.findByUsernameAndDeletedAtIsNull(username.value()).map(this::toDomain);
    }

    @Override
    public List<User> findActiveByEmails(Collection<String> emails) {
        if (emails.isEmpty()) return List.of();
        return jpa.findActiveByEmailIn(emails).stream().map(this::toDomain).toList();
    }

    @Override
    public CursorPageResponse<User> searchUsers(
            @Nullable ScrollCursor cursor, int size, UserScrollFilter filter) {
        int fetchLimit = size + 1;

        var cursorCreatedAt = cursor != null ? cursor.createdAt() : null;
        var cursorId = cursor != null ? cursor.id().toString() : null;

        var query = filter.hasQuery() ? escapeLike(filter.query()) : null;

        List<UserJpaEntity> rows =
                jpa.findActiveKeyset(cursorCreatedAt, cursorId, query, fetchLimit);

        boolean hasNext = rows.size() > size;
        List<User> items = rows.stream().limit(size).map(this::toDomain).toList();

        return CursorPageResponse.of(items, size, hasNext);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private User toDomain(UserJpaEntity e) {
        String hash = e.getPasswordHash();
        return User.reconstitute(
                e.getId(),
                Email.of(e.getEmail()),
                hash != null ? HashedPassword.of(hash) : null,
                FullName.of(e.getFullName()),
                e.getUsername(),
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
                u.getUsername().map(Username::value).orElse(null),
                u.getAvatarUrl().orElse(null),
                u.getGoogleUid().orElse(null),
                u.getAuthProvider(),
                u.getPreferences().orElse(null),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                u.getDeletedAt().orElse(null));
    }
}
