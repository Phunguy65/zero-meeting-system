package io.github.phunguy65.zms.presentation.splash;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import io.github.phunguy65.zms.di.MainExecutor;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.auth.RefreshTokenUseCase;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * ViewModel managing the splash screen auto-login flow.
 *
 * <p>On initialization, checks if auto-login is eligible (tokens exist + rememberMe enabled),
 * attempts token refresh if so, and emits appropriate state for navigation.
 *
 * <p>State flow:
 * <ul>
 *   <li>Loading → check tokens + rememberMe</li>
 *   <li>AutoLoginAttempt → refresh token</li>
 *   <li>Success → NavigateToMain</li>
 *   <li>Failure → SessionExpired → NavigateToWelcome(sessionExpired=true)</li>
 *   <li>No session → NavigateToWelcome(sessionExpired=false)</li>
 * </ul>
 */
@HiltViewModel
public class SplashViewModel extends ViewModel {

    private static final long REFRESH_TIMEOUT_SECONDS = 10;

    private final SessionRepository sessionRepository;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final Executor mainExecutor;

    private final MutableLiveData<SplashState> state =
            new MutableLiveData<>(new SplashState.Loading());
    private CompletableFuture<?> activeFuture;

    @Inject
    public SplashViewModel(
            SessionRepository sessionRepository,
            RefreshTokenUseCase refreshTokenUseCase,
            @MainExecutor Executor mainExecutor) {
        this.sessionRepository = sessionRepository;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.mainExecutor = mainExecutor;
    }

    /** Returns the current splash state as LiveData for observation. */
    public LiveData<SplashState> getState() {
        return state;
    }

    /**
     * Starts the auto-login check.
     * Should be called after splash animation completes.
     */
    public void checkAutoLogin() {
        boolean hasTokens = sessionRepository.hasTokens();
        boolean rememberMe = sessionRepository.isRememberMe();

        if (!hasTokens || !rememberMe) {
            state.setValue(new SplashState.NavigateToWelcome(false));
            return;
        }

        state.setValue(new SplashState.AutoLoginAttempt());
        attemptTokenRefresh();
    }

    private void attemptTokenRefresh() {
        String refreshToken = sessionRepository.getRefreshToken();
        if (refreshToken == null) {
            clearSessionAndNavigate();
            return;
        }

        activeFuture = refreshTokenUseCase
                .execute(refreshToken)
                .orTimeout(REFRESH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenAcceptAsync(
                        result -> {
                            sessionRepository.saveTokens(
                                    result.accessToken(), result.refreshToken());
                            state.setValue(new SplashState.NavigateToMain());
                        },
                        mainExecutor)
                .exceptionally(ex -> {
                    handleRefreshError(ex);
                    return null;
                });
    }

    private void handleRefreshError(Throwable ex) {
        Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;

        if (cause instanceof CancellationException) {
            return;
        }

        mainExecutor.execute(this::showSessionExpired);
    }

    private void showSessionExpired() {
        sessionRepository.clearAllSessionData();

        state.setValue(new SplashState.SessionExpired());
    }

    private void clearSessionAndNavigate() {
        sessionRepository.clearAllSessionData();
        state.setValue(new SplashState.NavigateToWelcome(false));
    }

    /**
     * Called after the session expired message has been displayed.
     * Transitions to NavigateToWelcome state with session expired flag.
     */
    public void onSessionExpiredDisplayed() {
        state.setValue(new SplashState.NavigateToWelcome(true));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (activeFuture != null && !activeFuture.isDone()) {
            activeFuture.cancel(true);
        }
    }
}
