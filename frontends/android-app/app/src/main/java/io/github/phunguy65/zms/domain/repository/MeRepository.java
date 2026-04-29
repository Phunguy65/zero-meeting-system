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

    /**
     * Replaces the current authenticated user's profile (PUT semantics).
     *
     * <p>All fields are required. Use {@code null} for {@code avatarUrl} to clear the avatar.
     *
     * @param fullName the full name (required, non-null)
     * @param username the username (required, non-null)
     * @param avatarUrl the avatar URL, or null to clear the avatar
     * @return a future that completes with the updated user profile
     */
    CompletableFuture<User> updateMe(String fullName, String username, String avatarUrl);

    /**
     * Permanently deletes the current authenticated user's account.
     *
     * @return a future that completes with {@link Void} on successful deletion
     */
    CompletableFuture<Void> deleteMe();
}
