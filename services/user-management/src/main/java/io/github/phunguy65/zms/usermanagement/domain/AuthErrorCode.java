package io.github.phunguy65.zms.usermanagement.domain;

import io.github.phunguy65.zms.shared.infrastructure.web.ErrorCode;

/**
 * Typed error codes for all authentication and user-management failures.
 *
 * <p>Controllers and use cases reference these constants only — no hardcoded strings anywhere.
 */
public enum AuthErrorCode implements ErrorCode {

    /** Registration attempt with an email address already in use. */
    EMAIL_ALREADY_EXISTS,

    /** Login failed due to wrong email or password (no user enumeration). */
    INVALID_CREDENTIALS,

    /** Refresh / logout token not found by its SHA-256 hash. */
    REFRESH_TOKEN_NOT_FOUND,

    /** Refresh token has passed its {@code expires_at} timestamp. */
    REFRESH_TOKEN_EXPIRED,

    /** Refresh token has already been revoked ({@code revoked_at IS NOT NULL}). */
    REFRESH_TOKEN_REVOKED,

    /** A previously revoked token was presented — possible token theft detected. */
    REFRESH_TOKEN_REUSE_DETECTED,

    /** Operation requires a user that does not exist. */
    USER_NOT_FOUND,

    /** Deleted user attempted login or JWT check failed because account is soft-deleted. */
    USER_DELETED,

    /** Firebase ID token failed verification (expired, malformed, wrong audience, etc.). */
    INVALID_FIREBASE_TOKEN,

    /** Firebase Admin SDK returned an unexpected error during token verification. */
    FIREBASE_AUTH_ERROR
}
