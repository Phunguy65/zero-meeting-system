package io.github.phunguy65.zms.presentation.common.state;

import org.jspecify.annotations.Nullable;

/**
 * Represents a single field-level validation failure surfaced to the UI.
 *
 * <p>Two construction patterns are supported:
 * <ul>
 *   <li><b>3-arg (backend)</b>: {@code new FieldError("email", "translated message", "REQUIRED")}
 *       — the {@code message} is already translated by {@link
 *       io.github.phunguy65.zms.data.remote.interceptor.ErrorTranslator} at the interceptor layer.</li>
 *   <li><b>2-arg (client-side)</b>: {@code new FieldError("email", "REQUIRED")} — sets
 *       {@code message = null}; the Fragment resolves a localized message from the {@code code}
 *       via {@code getString(R.string.*)}.</li>
 * </ul>
 *
 * @param field   the request field that failed validation (e.g. {@code "email"})
 * @param message human-readable description, or {@code null} for client-side validation errors
 * @param code    machine-readable violation category code (e.g. {@code "REQUIRED"})
 */
public record FieldError(String field, @Nullable String message, String code) {

    /**
     * Convenience constructor for client-side validation errors.
     *
     * <p>Sets {@code message} to {@code null}, signalling the UI layer to resolve
     * a localized message from the {@code code}.
     *
     * @param field the request field that failed validation
     * @param code  machine-readable violation category code
     */
    public FieldError(String field, String code) {
        this(field, null, code);
    }
}
