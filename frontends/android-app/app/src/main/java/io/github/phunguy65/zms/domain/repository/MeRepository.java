package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.User;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for current user (me) operations.
 *
 * <p>All methods return {@link CompletableFuture} to support non-blocking async execution.
 */
public interface MeRepository {

    /**
     * Gets the current authenticated user's profile.
     *
     * @return a future that completes with the user profile
     */
    CompletableFuture<User> getMe();
}
