package io.github.phunguy65.zms.presentation.common.state;

import java.util.List;

/**
 * Typed error hierarchy for UI consumption.
 *
 * <p>ViewModels map interceptor exceptions to these types so the UI layer can pattern-match
 * and display contextually appropriate error feedback:
 *
 * <ul>
 *   <li>{@link Fail} — business/validation error (JSend {@code "fail"}, HTTP 4xx).
 *       Contains a machine-readable {@code code}, a translated message, and optional
 *       field-level {@link FieldError}s for inline form validation.</li>
 *   <li>{@link ServerError} — unexpected server error (JSend {@code "error"}, HTTP 5xx).
 *       Contains only a human-readable message.</li>
 *   <li>{@link NetworkError} — connectivity failure (no network, timeout, DNS).
 *       UI should offer a retry action.</li>
 *   <li>{@link Unknown} — catch-all for unexpected exceptions.</li>
 * </ul>
 *
 * <p>Mapping example in a ViewModel:
 * <pre>{@code
 * catch (ApiFailException e) {
 *     var fieldErrors = e.getViolations().stream()
 *         .map(v -> new FieldError(v.field(), v.message(), v.code()))
 *         .toList();
 *     state.setValue(new UiState.Error<>(
 *         new UiError.Fail(e.getCode(), e.getMessage(), fieldErrors)));
 * }
 * catch (ApiErrorException e) {
 *     state.setValue(new UiState.Error<>(new UiError.ServerError(e.getMessage())));
 * }
 * catch (IOException e) {
 *     state.setValue(new UiState.Error<>(new UiError.NetworkError(e.getMessage())));
 * }
 * }</pre>
 */
public sealed interface UiError
        permits UiError.Fail, UiError.ServerError, UiError.NetworkError, UiError.Unknown {

    /**
     * Business or validation error from JSend {@code "fail"} responses (HTTP 4xx).
     *
     * @param code        machine-readable error code (e.g. {@code "EMAIL_ALREADY_EXISTS"})
     * @param message     human-readable, possibly translated message
     * @param fieldErrors field-level validation violations; empty list for domain errors
     */
    record Fail(String code, String message, List<FieldError> fieldErrors) implements UiError {
        /** Compact constructor — ensures the field error list is immutable. */
        public Fail {
            fieldErrors = List.copyOf(fieldErrors);
        }
    }

    /**
     * Unexpected server error from JSend {@code "error"} responses (HTTP 5xx).
     *
     * @param message human-readable error description
     */
    record ServerError(String message) implements UiError {}

    /**
     * Network connectivity failure (no connection, timeout, DNS resolution).
     *
     * @param message human-readable description of the network issue
     */
    record NetworkError(String message) implements UiError {}

    /**
     * Catch-all for unexpected or unclassified exceptions.
     *
     * @param message human-readable error description
     */
    record Unknown(String message) implements UiError {}
}
