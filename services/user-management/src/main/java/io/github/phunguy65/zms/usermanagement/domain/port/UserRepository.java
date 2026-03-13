package io.github.phunguy65.zms.usermanagement.domain.port;

import io.github.phunguy65.zms.shared.domain.CursorPageResult;
import io.github.phunguy65.zms.shared.domain.ScrollCursor;
import io.github.phunguy65.zms.usermanagement.domain.model.Email;
import io.github.phunguy65.zms.usermanagement.domain.model.User;
import io.github.phunguy65.zms.usermanagement.domain.model.Username;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Outbound port: persistence operations for the {@link User} aggregate. */
public interface UserRepository {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UUID id);

    /** Returns the user only if {@code deleted_at IS NULL}. */
    Optional<User> findActiveById(UUID id);

    /** Returns the user only if {@code deleted_at IS NULL}. */
    Optional<User> findActiveByEmail(Email email);

    /** Returns the active user with the given Firebase Google UID, or empty if not found. */
    Optional<User> findActiveByGoogleUid(String googleUid);

    User save(User user);

    boolean existsByEmail(Email email);

    /** Returns {@code true} only if an active (non-deleted) user with this email exists. */
    boolean existsActiveByEmail(Email email);

    /** Returns {@code true} only if an active (non-deleted) user with this username exists. */
    boolean existsActiveByUsername(Username username);

    /** Returns the active user with the given username, or empty if not found. */
    Optional<User> findActiveByUsername(Username username);

    /**
     * Returns a keyset-scrolled page of active (non-deleted) users matching the given filter.
     * Results are ordered by {@code (created_at DESC, id DESC)}.
     *
     * @param cursor decoded cursor from the previous page, or {@code null} for the first page
     * @param size   page size (max 100)
     * @param filter optional search filter; use {@link UserScrollFilter#empty()} for no filtering
     */
    CursorPageResult<User> searchUsers(
            @Nullable ScrollCursor cursor, int size, UserScrollFilter filter);
}
