package io.github.phunguy65.zms.domain.repository;

import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.model.RegisterResult;
import java.util.concurrent.CompletableFuture;

/**
 * Repository interface for authentication operations.
 *
 * <p>All methods return {@link CompletableFuture} to support non-blocking async execution. Callers
 * should handle completion via {@code thenAccept}, {@code thenApply}, or {@code exceptionally}.
 */
public interface AuthRepository {

    /**
     * Authenticates a user with email and password.
     *
     * @param email user's email address
     * @param password user's password
     * @return a future that completes with login result containing tokens
     */
    CompletableFuture<LoginResult> login(String email, String password);

    /**
     * Registers a new user account.
     *
     * @param fullName user's display name
     * @param username chosen username
     * @param email email address
     * @param password chosen password
     * @return a future that completes with registration result
     */
    CompletableFuture<RegisterResult> register(
            String fullName, String username, String email, String password);

    /**
     * Authenticates or registers a user via Google (Firebase ID token).
     *
     * @param idToken Firebase ID token obtained from Firebase Auth SDK
     * @return a future that completes with login result containing tokens
     */
    CompletableFuture<LoginResult> googleLogin(String idToken);
}
