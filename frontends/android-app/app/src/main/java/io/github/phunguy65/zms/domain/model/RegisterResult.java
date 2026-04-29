package io.github.phunguy65.zms.domain.model;

import java.util.UUID;

/**
 * Domain result for a successful user registration.
 *
 * @param userId   server-assigned user ID
 * @param email    registered email address
 * @param fullName user's display name
 * @param username chosen username
 */
public record RegisterResult(UUID userId, String email, String fullName, String username) {}
