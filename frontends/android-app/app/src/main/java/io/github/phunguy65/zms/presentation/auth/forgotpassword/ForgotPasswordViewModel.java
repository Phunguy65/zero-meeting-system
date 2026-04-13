package io.github.phunguy65.zms.presentation.auth.forgotpassword;

import android.util.Patterns;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.usecase.auth.RequestPasswordResetUseCase;
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
 * ViewModel managing the forgot password (request OTP) flow.
 *
 * <p>Validates email format client-side, then calls the backend to send an OTP email. On success,
 * the UI should navigate to the reset password screen.
 */
@HiltViewModel
public class ForgotPasswordViewModel extends ViewModel {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final Executor mainExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<UiState<String>> requestState =
            new MutableLiveData<>(new UiState.Idle<>());

    @Inject
    public ForgotPasswordViewModel(
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            @MainExecutor Executor mainExecutor) {
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.mainExecutor = mainExecutor;
    }

    public LiveData<UiState<String>> getRequestState() {
        return requestState;
    }

    /**
     * Requests a password reset OTP for the given email.
     *
     * <p>Validates email format first. On validation failure, posts {@link UiState.Error} with
     * field-level error. On success from backend, posts {@link UiState.Success} with the email
     * (to pass to reset screen).
     *
     * @param email the user's email address
     */
    public void requestPasswordReset(String email) {
        List<FieldError> fieldErrors = new ArrayList<>();

        if (email == null || email.isBlank()) {
            fieldErrors.add(new FieldError("email", "REQUIRED"));
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            fieldErrors.add(new FieldError("email", "FORMAT"));
        }

        if (!fieldErrors.isEmpty()) {
            requestState.setValue(
                    new UiState.Error<>(new UiError.Fail("VALIDATION", null, fieldErrors)));
            return;
        }

        requestState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = requestPasswordResetUseCase
                .execute(email)
                .thenAcceptAsync(
                        ignored -> {
                            requestState.setValue(new UiState.Success<>(email));
                        },
                        mainExecutor)
                .exceptionally(ex -> {
                    handleError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /** Resets the state to Idle (e.g., after consuming an error or navigating away). */
    public void resetState() {
        requestState.setValue(new UiState.Idle<>());
    }

    /**
     * Handles exceptions from async operations by mapping them to appropriate UI error states.
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

        mainExecutor.execute(() -> requestState.setValue(new UiState.Error<>(error)));
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
