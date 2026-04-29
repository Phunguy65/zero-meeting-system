package io.github.phunguy65.zms.presentation.auth.forgotpassword;

import android.os.CountDownTimer;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.data.remote.interceptor.ApiErrorException;
import io.github.phunguy65.zms.data.remote.interceptor.ApiFailException;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.usecase.auth.RequestPasswordResetUseCase;
import io.github.phunguy65.zms.domain.usecase.auth.ResetPasswordUseCase;
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
 * ViewModel managing the reset password flow (OTP verification + new password).
 *
 * <p>Features:
 * <ul>
 *   <li>OTP and password validation</li>
 *   <li>60-second resend cooldown timer</li>
 *   <li>Password reset API call</li>
 *   <li>Resend OTP functionality</li>
 * </ul>
 */
@HiltViewModel
public class ResetPasswordViewModel extends ViewModel {

    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final ResetPasswordUseCase resetPasswordUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final Executor mainExecutor;

    private final List<CompletableFuture<?>> activeFutures = new ArrayList<>();

    private final MutableLiveData<UiState<Void>> resetState =
            new MutableLiveData<>(new UiState.Idle<>());

    private final MutableLiveData<UiState<Void>> resendState =
            new MutableLiveData<>(new UiState.Idle<>());

    private final MutableLiveData<Integer> resendCooldown = new MutableLiveData<>(0);

    private CountDownTimer resendTimer;

    @Inject
    public ResetPasswordViewModel(
            ResetPasswordUseCase resetPasswordUseCase,
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            @MainExecutor Executor mainExecutor) {
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.mainExecutor = mainExecutor;

        startResendCooldown();
    }

    public LiveData<UiState<Void>> getResetState() {
        return resetState;
    }

    public LiveData<UiState<Void>> getResendState() {
        return resendState;
    }

    /** Seconds remaining until resend is allowed. 0 means resend is enabled. */
    public LiveData<Integer> getResendCooldown() {
        return resendCooldown;
    }

    /**
     * Validates and submits the password reset.
     *
     * @param email       the user's email
     * @param otp         the 6-digit OTP
     * @param newPassword the new password
     * @param confirmPassword confirmation of the new password
     */
    public void resetPassword(
            String email, String otp, String newPassword, String confirmPassword) {
        List<FieldError> fieldErrors = new ArrayList<>();

        if (otp == null || otp.isBlank()) {
            fieldErrors.add(new FieldError("otp", "REQUIRED"));
        } else if (!otp.matches("\\d{6}")) {
            fieldErrors.add(new FieldError("otp", "FORMAT"));
        }

        if (newPassword == null || newPassword.isBlank()) {
            fieldErrors.add(new FieldError("newPassword", "REQUIRED"));
        } else if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            fieldErrors.add(new FieldError("newPassword", "MIN_LENGTH"));
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            fieldErrors.add(new FieldError("confirmPassword", "REQUIRED"));
        } else if (!confirmPassword.equals(newPassword)) {
            fieldErrors.add(new FieldError("confirmPassword", "MISMATCH"));
        }

        if (!fieldErrors.isEmpty()) {
            resetState.setValue(
                    new UiState.Error<>(new UiError.Fail("VALIDATION", null, fieldErrors)));
            return;
        }

        resetState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = resetPasswordUseCase
                .execute(email, otp, newPassword)
                .thenAcceptAsync(
                        ignored -> resetState.setValue(new UiState.Success<>(null)), mainExecutor)
                .exceptionally(ex -> {
                    handleResetError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /**
     * Resends the OTP to the given email address.
     * Only allowed when cooldown is 0.
     */
    public void resendOtp(String email) {
        Integer cooldown = resendCooldown.getValue();
        if (cooldown != null && cooldown > 0) {
            return;
        }

        resendState.setValue(new UiState.Loading<>());

        CompletableFuture<?> future = requestPasswordResetUseCase
                .execute(email)
                .thenAcceptAsync(
                        ignored -> {
                            resendState.setValue(new UiState.Success<>(null));
                            startResendCooldown();
                        },
                        mainExecutor)
                .exceptionally(ex -> {
                    handleResendError(ex);
                    return null;
                });

        trackFuture(future);
    }

    /** Resets the reset state to Idle. */
    public void resetResetState() {
        resetState.setValue(new UiState.Idle<>());
    }

    /** Resets the resend state to Idle. */
    public void resetResendState() {
        resendState.setValue(new UiState.Idle<>());
    }

    private void startResendCooldown() {
        if (resendTimer != null) {
            resendTimer.cancel();
        }

        resendCooldown.setValue(RESEND_COOLDOWN_SECONDS);

        resendTimer = new CountDownTimer(RESEND_COOLDOWN_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendCooldown.setValue((int) (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                resendCooldown.setValue(0);
            }
        };
        resendTimer.start();
    }

    private void handleResetError(Throwable ex) {
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

        mainExecutor.execute(() -> resetState.setValue(new UiState.Error<>(error)));
    }

    private void handleResendError(Throwable ex) {
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

        mainExecutor.execute(() -> resendState.setValue(new UiState.Error<>(error)));
    }

    private void trackFuture(CompletableFuture<?> future) {
        activeFutures.add(future);
        future.whenComplete((result, ex) -> activeFutures.remove(future));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
        for (CompletableFuture<?> future : activeFutures) {
            if (!future.isDone()) {
                future.cancel(true);
            }
        }
        activeFutures.clear();
    }
}
