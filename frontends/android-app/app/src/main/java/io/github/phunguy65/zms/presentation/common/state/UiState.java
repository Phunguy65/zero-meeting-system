package io.github.phunguy65.zms.presentation.common.state;

/**
 * Sealed interface representing UI state for async operations.
 *
 * <p>Usage: {@code UiState<User>} can be one of:
 * <ul>
 *   <li>{@link Loading} — operation in progress</li>
 *   <li>{@link Success} — operation succeeded with data</li>
 *   <li>{@link Error} — operation failed with a message</li>
 * </ul>
 */
public sealed interface UiState<T> permits UiState.Loading, UiState.Success, UiState.Error {

    record Loading<T>() implements UiState<T> {}

    record Success<T>(T data) implements UiState<T> {}

    record Error<T>(String message) implements UiState<T> {}
}
