package io.github.phunguy65.zms.domain.model;

/**
 * Domain result for a successful login or Google Sign-In.
 *
 * @param accessToken  JWT access token issued by the backend
 * @param refreshToken opaque refresh token for token rotation
 * @param expiresIn    access token lifetime in seconds
 */
public record LoginResult(String accessToken, String refreshToken, long expiresIn) {}
