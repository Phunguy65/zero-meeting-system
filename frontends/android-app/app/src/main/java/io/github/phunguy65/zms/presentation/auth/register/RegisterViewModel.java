package io.github.phunguy65.zms.presentation.auth.register;

import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.RegisterResult;
import io.github.phunguy65.zms.domain.usecase.auth.RegisterUseCase;
import io.github.phunguy65.zms.presentation.common.state.FieldError;
import io.github.phunguy65.zms.presentation.common.state.UiError;
import io.github.phunguy65.zms.presentation.common.state.UiState;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jspecify.annotations.Nullable;

/**
 * ViewModel managing registration state with client-side validation.
 *
 * <p>Uses {@link CompletableFuture} for async operations with proper cancellation support. All
 * active futures are tracked and cancelled when the ViewModel is cleared.
 *
 * <p>Validation rules match server-side constraints:
 *
 * <ul>
 *   <li>fullName: required, max 255 chars
 *   <li>username: required, 3-30 chars, pattern {@code ^[a-zA-Z0-9_-]+$}
 *   <li>email: required, valid email format
 *   <li>password: required, 8-128 chars
 *   <li>confirmPassword: required, must match password
 * </ul>
 */
@HiltViewModel
public class RegisterViewModel extends ViewModel {

    private static final int FULLNAME_MAX_LENGTH = 255;
    private static final int USERNAME_MIN_LENGTH = 3;
    private static final int USERNAME_MAX_LENGTH = 30;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 128;

    private final RegisterUseCase registerUseCase;
    private final Executor mainExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<UiState<RegisterResult>> registerState =
            new MutableLiveData<>(new UiState.Idle<>());

    @Inject
    public RegisterViewModel(RegisterUseCase registerUseCase, @MainExecutor Executor mainExecutor) {
        this.registerUseCase = registerUseCase;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<UiState<RegisterResult>> getRegisterState() {
        return registerState;
    }

    /**
     * Validates fullName field.
     *
     * @return FieldError if invalid, null if valid
     */
    public @Nullable FieldError validateFullName(@Nullable String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new FieldError("fullName", "REQUIRED");
        }
        if (fullName.length() > FULLNAME_MAX_LENGTH) {
            return new FieldError("fullName", "FULLNAME_TOO_LONG");
        }
        return null;
    }

    /**
     * Validates username field.
     *
     * @return FieldError if invalid, null if valid
     */
    public @Nullable FieldError validateUsername(@Nullable String username) {
        if (username == null || username.isBlank()) {
            return new FieldError("username", "REQUIRED");
        }
        if (username.length() < USERNAME_MIN_LENGTH) {
            return new FieldError("username", "USERNAME_TOO_SHORT");
        }
        if (username.length() > USERNAME_MAX_LENGTH) {
            return new FieldError("username", "USERNAME_TOO_LONG");
        }
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            return new FieldError("username", "USERNAME_FORMAT");
        }
        return null;
    }

    /**
     * Validates email field.
     *
     * @return FieldError if invalid, null if valid
     */
    public @Nullable FieldError validateEmail(@Nullable String email) {
        if (email == null || email.isBlank()) {
            return new FieldError("email", "REQUIRED");
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return new FieldError("email", "FORMAT");
        }
        return null;
    }

    /**
     * Validates password field.
     *
     * @return FieldError if invalid, null if valid
     */
    public @Nullable FieldError validatePassword(@Nullable String password) {
        if (password == null || password.isBlank()) {
            return new FieldError("password", "REQUIRED");
        }
        if (password.length() < PASSWORD_MIN_LENGTH) {
            return new FieldError("password", "PASSWORD_TOO_SHORT");
        }
        if (password.length() > PASSWORD_MAX_LENGTH) {
            return new FieldError("password", "PASSWORD_TOO_LONG");
        }
        return null;
    }

    /**
     * Validates confirmPassword field.
     *
     * @return FieldError if invalid, null if valid
     */
    public @Nullable FieldError validateConfirmPassword(
            @Nullable String confirmPassword, @Nullable String password) {
        if (confirmPassword == null || confirmPassword.isBlank()) {
            return new FieldError("confirmPassword", "REQUIRED");
        }
        if (password != null && !password.equals(confirmPassword)) {
            return new FieldError("confirmPassword", "MISMATCH");
        }
        return null;
    }

    /**
     * Attempts registration after client-side validation.
     *
     * @param fullName user's display name
     * @param username chosen username
     * @param email email address
     * @param password chosen password
     * @param confirmPassword password confirmation
     */
    public void register(
            String fullName,
            String username,
            String email,
            String password,
            String confirmPassword) {

        List<FieldError> fieldErrors = new ArrayList<>();

        FieldError fullNameError = validateFullName(fullName);
        if (fullNameError != null) fieldErrors.add(fullNameError);

        FieldError usernameError = validateUsername(username);
        if (usernameError != null) fieldErrors.add(usernameError);

        FieldError emailError = validateEmail(email);
        if (emailError != null) fieldErrors.add(emailError);

        FieldError passwordError = validatePassword(password);
        if (passwordError != null) fieldErrors.add(passwordError);

        FieldError confirmError = validateConfirmPassword(confirmPassword, password);
        if (confirmError != null) fieldErrors.add(confirmError);

        if (!fieldErrors.isEmpty()) {
            registerState.setValue(
                    new UiState.Error<>(new UiError.Fail("VALIDATION", null, fieldErrors)));
            return;
        }

        registerState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = registerUseCase
                .execute(fullName, username, email, password)
                .thenAcceptAsync(
                        result -> registerState.setValue(new UiState.Success<>(result)),
                        mainExecutor)
                .exceptionally(ex -> {
                    handleError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /** Resets the register state to Idle. */
    public void resetState() {
        registerState.setValue(new UiState.Idle<>());
    }

    /**
     * Handles exceptions from async operations by mapping them to appropriate UI error states.
     *
     * <p>Unwraps {@link CompletionException} to get the root cause. Ignores {@link
     * CancellationException} as it indicates intentional cancellation during cleanup.
     */
    private void handleError(Throwable ex) {
        Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;

        // Ignore cancellation - expected during ViewModel cleanup
        if (cause instanceof CancellationException) {
            return;
        }

        UiError error;
        if (cause instanceof ApiFailException e) {
            List<FieldError> errs = e.getViolations().stream()
                    .map(v -> new FieldError(v.field(), v.message(), v.code()))
                    .collect(Collectors.toList());
            error = new UiError.Fail(e.getCode(), e.getMessage(), errs);
        } else if (cause instanceof ApiErrorException) {
            error = new UiError.ServerError(null);
        } else if (cause instanceof IOException) {
            error = new UiError.NetworkError(null);
        } else {
            error = new UiError.Unknown(null);
        }

        mainExecutor.execute(() -> registerState.setValue(new UiState.Error<>(error)));
    }

    /** Tracks a future for cancellation when ViewModel is cleared. */
    private void trackFuture(CompletableFuture<?> future) {
        activeFutures.add(future);
        future.whenComplete((result, ex) -> activeFutures.remove(future));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        for (CompletableFuture<?> future : activeFutures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        activeFutures.clear();
    }
}
