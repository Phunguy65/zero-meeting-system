package infrastructure.web;

/**
 * Machine-readable error codes for all JSend {@code fail} responses.
 *
 * <p>Constants are module-namespaced (e.g. {@code USER_}, {@code BOOKING_}) to prevent collisions
 * and make provenance clear. {@code VALIDATION_ERROR} is the umbrella code used when Bean
 * Validation fails; individual field violations carry a {@link ViolationCode}.
 */
public enum ErrorCode {}
