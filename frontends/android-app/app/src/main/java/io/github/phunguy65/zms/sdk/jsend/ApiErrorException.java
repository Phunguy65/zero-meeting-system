package io.github.phunguy65.zms.sdk.jsend;

/**
 * Thrown when the server returns a JSend {@code "error"} response (HTTP 5xx).
 *
 * <p>Contains only a human-readable message; the server does not expose
 * machine-readable error codes for 5xx failures.
 */
public final class ApiErrorException extends RuntimeException {

    public ApiErrorException(String message) {
        super(message);
    }
}
