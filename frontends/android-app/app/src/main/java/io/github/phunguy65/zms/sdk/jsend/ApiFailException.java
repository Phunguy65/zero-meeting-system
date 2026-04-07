package io.github.phunguy65.zms.sdk.jsend;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when the server returns a JSend {@code "fail"} response (HTTP 4xx).
 *
 * <p>Contains the machine-readable {@code code}, a human-readable {@code message},
 * and an optional list of field-level {@link Violation}s.
 */
public final class ApiFailException extends RuntimeException {

    private final String code;
    private final List<Violation> violations;

    public ApiFailException(String code, String message, List<Violation> violations) {
        super(message);
        this.code = code;
        this.violations = violations != null
                ? Collections.unmodifiableList(violations)
                : Collections.emptyList();
    }

    /** Machine-readable error code (e.g. {@code "EMAIL_EXISTS"}). */
    public String getCode() {
        return code;
    }

    /** Field-level validation violations (empty for domain errors). */
    public List<Violation> getViolations() {
        return violations;
    }

    /**
     * Single field-level validation failure.
     *
     * @param field   the request field that failed validation
     * @param message human-readable description of the failure
     * @param code    machine-readable violation category code
     */
    public record Violation(String field, String message, String code) {}
}
