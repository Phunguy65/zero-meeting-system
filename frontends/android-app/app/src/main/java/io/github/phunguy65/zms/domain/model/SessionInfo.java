package io.github.phunguy65.zms.domain.model;

/**
 * Represents the cached user session information.
 *
 * <p>This domain model contains user profile data that is persisted locally
 * for auto-login and display purposes.
 *
 * @param userId the unique user identifier
 * @param email the user's email address
 * @param fullName the user's display name
 * @param username the user's unique username
 * @param avatarUrl the URL to the user's avatar image (may be null)
 */
public record SessionInfo(
        String userId, String email, String fullName, String username, String avatarUrl) {

    /**
     * Creates a SessionInfo from a User domain model.
     *
     * @param user the user to convert
     * @return a new SessionInfo
     */
    public static SessionInfo fromUser(User user) {
        return new SessionInfo(
                user.id(), user.email(), user.fullName(), user.username(), user.avatarUrl());
    }
}
