package io.github.phunguy65.zms.usermanagement.domain;

import io.github.phunguy65.zms.shared.domain.ErrorCode;

/**
 * Machine-readable error codes for all authentication and user-management failures.
 *
 * <p>These constants are used as the {@code code} field in JSend {@code fail} responses.
 * The mapping from {@link AuthError} sealed records to these codes lives in
 * {@code BaseController#errorResponse}.
 */
public enum AuthErrorCode implements ErrorCode {
    EMAIL_ALREADY_EXISTS,
    USERNAME_ALREADY_EXISTS,
    INVALID_CREDENTIALS,
    REFRESH_TOKEN_NOT_FOUND,
    REFRESH_TOKEN_EXPIRED,
    REFRESH_TOKEN_REVOKED,
    REFRESH_TOKEN_REUSE_DETECTED,
    USER_NOT_FOUND,
    USER_DELETED,
    INVALID_FIREBASE_TOKEN,
    FIREBASE_AUTH_ERROR,
    PREFERENCES_SERIALIZATION_ERROR
}
