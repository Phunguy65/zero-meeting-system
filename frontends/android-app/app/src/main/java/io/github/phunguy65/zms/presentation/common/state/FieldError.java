package io.github.phunguy65.zms.presentation.common.state;

/**
 * Represents a single field-level validation failure surfaced to the UI.
 *
 * <p>Maps directly from {@link io.github.phunguy65.zms.data.remote.interceptor.ApiFailException.Violation}
 * at the ViewModel layer.
 *
 * @param field   the request field that failed validation (e.g. {@code "email"})
 * @param message human-readable description of why validation failed
 * @param code    machine-readable violation category code (e.g. {@code "REQUIRED"})
 */
public record FieldError(String field, String message, String code) {}
