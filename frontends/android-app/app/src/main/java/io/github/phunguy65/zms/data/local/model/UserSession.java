package io.github.phunguy65.zms.data.local.model;

/**
 * Represents the persisted user session data stored in SharedPreferences.
 *
 * <p>This record holds user profile information retrieved from the API after login,
 * along with the rememberMe flag that controls auto-login behavior.
 *
 * @param userId the unique user identifier
 * @param email the user's email address
 * @param fullName the user's display name
 * @param username the user's unique username
 * @param avatarUrl the URL to the user's avatar image (may be null)
 * @param rememberMe whether the user chose to stay logged in
 */
public record UserSession(
        String userId,
        String email,
        String fullName,
        String username,
        String avatarUrl,
        boolean rememberMe) {

    /**
     * Creates a UserSession with the rememberMe flag set to true.
     *
     * @param userId the unique user identifier
     * @param email the user's email address
     * @param fullName the user's display name
     * @param username the user's unique username
     * @param avatarUrl the URL to the user's avatar image
     * @return a new UserSession with rememberMe enabled
     */
    public static UserSession withRememberMe(
            String userId, String email, String fullName, String username, String avatarUrl) {
        return new UserSession(userId, email, fullName, username, avatarUrl, true);
    }
}
