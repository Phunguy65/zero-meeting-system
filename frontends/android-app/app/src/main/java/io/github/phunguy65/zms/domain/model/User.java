package io.github.phunguy65.zms.domain.model;

/**
 * Domain entity representing an authenticated user.
 *
 * @param id the unique user identifier (UUID)
 * @param email the user's email address
 * @param fullName the user's display name
 * @param username the user's unique username
 * @param avatarUrl the URL to the user's avatar image (may be null)
 */
public record User(String id, String email, String fullName, String username, String avatarUrl) {}
