package io.github.phunguy65.zms.shared.infrastructure.web;

/**
 * Marker interface for machine-readable error codes used in JSend {@code fail} responses.
 *
 * <p>Each service defines its own enum implementing this interface (e.g. {@code AuthErrorCode}).
 * Constants are module-namespaced to prevent collisions. {@link CommonErrorCode#VALIDATION_ERROR}
 * is the umbrella code used when Bean Validation fails; individual field violations carry a
 * {@link ViolationCode}.
 */
public interface ErrorCode {}
