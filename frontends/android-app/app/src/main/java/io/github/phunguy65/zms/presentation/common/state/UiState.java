package io.github.phunguy65.zms.presentation.common.state;

/**
 * Sealed interface representing UI state for async operations.
 *
 * <p>Usage: {@code UiState<User>} can be one of:
 * <ul>
 *   <li>{@link Idle} — no operation started yet (initial state)</li>
 *   <li>{@link Loading} — operation in progress</li>
 *   <li>{@link Success} — operation succeeded with data</li>
 *   <li>{@link Error} — operation failed with a typed {@link UiError}</li>
 * </ul>
 *
 * <p>The {@link Error} variant wraps a {@link UiError} sealed hierarchy rather than a plain
 * {@code String} message, preserving structured error information (error code, field-level
 * violations) from the JSend API responses through to the UI layer.
 */
public sealed interface UiState<T>
        permits UiState.Idle, UiState.Loading, UiState.Success, UiState.Error {

    /** No operation started yet. This is the default initial state. */
    record Idle<T>() implements UiState<T> {}

    /** An async operation is in progress. */
    record Loading<T>() implements UiState<T> {}

    /** The operation completed successfully with the given data. */
    record Success<T>(T data) implements UiState<T> {}

    /** The operation failed. See {@link UiError} for the error type hierarchy. */
    record Error<T>(UiError error) implements UiState<T> {}
}
