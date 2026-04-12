package io.github.phunguy65.zms.presentation.auth.login;

import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.local.TokenManager;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.usecase.auth.GoogleLoginUseCase;
import io.github.phunguy65.zms.domain.usecase.auth.LoginUseCase;
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
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * ViewModel managing login state for both email/password and Google Sign-In flows.
 *
 * <p>Uses {@link CompletableFuture} for async operations with proper cancellation support. All
 * active futures are tracked and cancelled when the ViewModel is cleared.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final LoginUseCase loginUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final TokenManager tokenManager;
    private final Executor mainExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<UiState<LoginResult>> loginState =
            new MutableLiveData<>(new UiState.Idle<>());

    @Inject
    public LoginViewModel(
            LoginUseCase loginUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            TokenManager tokenManager,
            @MainExecutor Executor mainExecutor) {
        this.loginUseCase = loginUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.tokenManager = tokenManager;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<UiState<LoginResult>> getLoginState() {
        return loginState;
    }

    /**
     * Attempts email/password login after client-side validation.
     *
     * <p>Validates that email and password are non-empty and email format is valid. On validation
     * failure, posts {@link UiState.Error} with field-level errors. On success, stores tokens via
     * {@link TokenManager} and posts {@link UiState.Success}.
     */
    public void loginWithEmail(String email, String password) {
        List<FieldError> fieldErrors = new ArrayList<>();
        if (email == null || email.isBlank()) {
            fieldErrors.add(new FieldError("email", "REQUIRED"));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            fieldErrors.add(new FieldError("email", "FORMAT"));
        }
        if (password == null || password.isBlank()) {
            fieldErrors.add(new FieldError("password", "REQUIRED"));
        }
        if (!fieldErrors.isEmpty()) {
            loginState.setValue(
                    new UiState.Error<>(new UiError.Fail("VALIDATION", null, fieldErrors)));
            return;
        }

        loginState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = loginUseCase
                .execute(email, password)
                .thenAcceptAsync(
                        result -> {
                            tokenManager.saveTokens(result.accessToken(), result.refreshToken());
                            loginState.setValue(new UiState.Success<>(result));
                        },
                        mainExecutor)
                .exceptionally(ex -> {
                    handleError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /**
     * Handles login via Firebase ID token obtained from Google Sign-In.
     *
     * @param firebaseIdToken the Firebase ID token to send to the backend
     */
    public void loginWithGoogle(String firebaseIdToken) {
        loginState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = googleLoginUseCase
                .execute(firebaseIdToken)
                .thenAcceptAsync(
                        result -> {
                            tokenManager.saveTokens(result.accessToken(), result.refreshToken());
                            loginState.setValue(new UiState.Success<>(result));
                        },
                        mainExecutor)
                .exceptionally(ex -> {
                    handleError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /** Resets the login state to Idle (e.g. after consuming an error). */
    public void resetState() {
        loginState.setValue(new UiState.Idle<>());
    }

    /**
     * Handles exceptions from async operations by mapping them to appropriate UI error states.
     *
     * <p>Unwraps {@link CompletionException} to get the root cause. Ignores {@link
     * CancellationException} as it indicates intentional cancellation during cleanup.
     */
    private void handleError(Throwable ex) {
        Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;

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

        mainExecutor.execute(() -> loginState.setValue(new UiState.Error<>(error)));
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
