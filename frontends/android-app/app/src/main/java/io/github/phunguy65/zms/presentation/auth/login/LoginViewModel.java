package io.github.phunguy65.zms.presentation.auth.login;

import android.util.Log;
import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.IoExecutor;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.model.SessionInfo;
import io.github.phunguy65.zms.domain.model.User;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.auth.GoogleLoginUseCase;
import io.github.phunguy65.zms.domain.usecase.auth.LoginUseCase;
import io.github.phunguy65.zms.domain.usecase.me.GetMeUseCase;
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
 *
 * <p>When rememberMe is enabled, fetches user profile via {@link GetMeUseCase} after successful
 * login and persists the session data via {@link SessionRepository}.
 */
@HiltViewModel
public class LoginViewModel extends ViewModel {

    private static final String TAG = "LoginViewModel";

    private final LoginUseCase loginUseCase;
    private final GoogleLoginUseCase googleLoginUseCase;
    private final GetMeUseCase getMeUseCase;
    private final SessionRepository sessionRepository;
    private final Executor mainExecutor;
    private final Executor ioExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<UiState<LoginResult>> loginState =
            new MutableLiveData<>(new UiState.Idle<>());

    @Inject
    public LoginViewModel(
            LoginUseCase loginUseCase,
            GoogleLoginUseCase googleLoginUseCase,
            GetMeUseCase getMeUseCase,
            SessionRepository sessionRepository,
            @MainExecutor Executor mainExecutor,
            @IoExecutor Executor ioExecutor) {
        this.loginUseCase = loginUseCase;
        this.googleLoginUseCase = googleLoginUseCase;
        this.getMeUseCase = getMeUseCase;
        this.sessionRepository = sessionRepository;
        this.mainExecutor = mainExecutor;
        this.ioExecutor = ioExecutor;
    }

    public LiveData<UiState<LoginResult>> getLoginState() {
        return loginState;
    }

    /**
     * Attempts email/password login after client-side validation.
     *
     * <p>Validates that email and password are non-empty and email format is valid. On validation
     * failure, posts {@link UiState.Error} with field-level errors. On success, stores tokens via
     * {@link SessionRepository}, fetches user profile if rememberMe is true, and posts
     * {@link UiState.Success}.
     *
     * @param email user's email address
     * @param password user's password
     * @param rememberMe whether to persist session for auto-login
     */
    public void loginWithEmail(String email, String password, boolean rememberMe) {
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
                .thenComposeAsync(
                        result -> handleLoginSuccess(result, rememberMe),
                        ioExecutor)
                .thenAcceptAsync(
                        result -> loginState.setValue(new UiState.Success<>(result)),
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
     * @param rememberMe whether to persist session for auto-login
     */
    public void loginWithGoogle(String firebaseIdToken, boolean rememberMe) {
        loginState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = googleLoginUseCase
                .execute(firebaseIdToken)
                .thenComposeAsync(
                        result -> handleLoginSuccess(result, rememberMe),
                        ioExecutor)
                .thenAcceptAsync(
                        result -> loginState.setValue(new UiState.Success<>(result)),
                        mainExecutor)
                .exceptionally(ex -> {
                    handleError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /**
     * Handles successful login: saves tokens and optionally fetches/saves user profile.
     *
     * @param result the login result containing tokens
     * @param rememberMe whether to persist session
     * @return a future that completes with the login result
     */
    private CompletableFuture<LoginResult> handleLoginSuccess(LoginResult result, boolean rememberMe) {
        sessionRepository.saveTokens(result.accessToken(), result.refreshToken());

        if (rememberMe) {
            return getMeUseCase.execute()
                    .thenApply(user -> {
                        saveUserSession(user);
                        return result;
                    })
                    .exceptionally(ex -> {
                        Log.w(TAG, "Profile fetch failed after login, rememberMe set without profile", ex);
                        sessionRepository.setRememberMe(true);
                        return result;
                    });
        } else {
            sessionRepository.setRememberMe(false);
            return CompletableFuture.completedFuture(result);
        }
    }

    /**
     * Saves the user session to preferences.
     *
     * @param user the user profile to save
     */
    private void saveUserSession(User user) {
        SessionInfo sessionInfo = SessionInfo.fromUser(user);
        sessionRepository.saveSession(sessionInfo);
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
