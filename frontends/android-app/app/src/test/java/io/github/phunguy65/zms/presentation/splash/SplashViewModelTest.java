package io.github.phunguy65.zms.presentation.splash;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import io.github.phunguy65.zms.domain.model.LoginResult;
import io.github.phunguy65.zms.domain.repository.SessionRepository;
import io.github.phunguy65.zms.domain.usecase.auth.RefreshTokenUseCase;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link SplashViewModel}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SplashViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private RefreshTokenUseCase refreshTokenUseCase;

    // Immediate executor for testing
    private final Executor testExecutor = Runnable::run;

    private SplashViewModel viewModel;

    @Before
    public void setup() {
        viewModel = new SplashViewModel(sessionRepository, refreshTokenUseCase, testExecutor);
    }

    @Test
    public void initialState_isLoading() {
        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.Loading);
    }

    @Test
    public void checkAutoLogin_noTokens_emitsNavigateToWelcome() {
        when(sessionRepository.hasTokens()).thenReturn(false);
        when(sessionRepository.isRememberMe()).thenReturn(true);

        viewModel.checkAutoLogin();

        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.NavigateToWelcome);
        assertFalse(((SplashState.NavigateToWelcome) state).sessionExpired());
    }

    @Test
    public void checkAutoLogin_tokensButNoRememberMe_emitsNavigateToWelcome() {
        when(sessionRepository.hasTokens()).thenReturn(true);
        when(sessionRepository.isRememberMe()).thenReturn(false);

        viewModel.checkAutoLogin();

        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.NavigateToWelcome);
        assertFalse(((SplashState.NavigateToWelcome) state).sessionExpired());
    }

    @Test
    public void checkAutoLogin_eligible_emitsAutoLoginAttempt() {
        when(sessionRepository.hasTokens()).thenReturn(true);
        when(sessionRepository.isRememberMe()).thenReturn(true);
        when(sessionRepository.getRefreshToken()).thenReturn("refresh_token");
        
        // Return a future that never completes to capture the intermediate state
        CompletableFuture<LoginResult> pendingFuture = new CompletableFuture<>();
        when(refreshTokenUseCase.execute("refresh_token")).thenReturn(pendingFuture);

        viewModel.checkAutoLogin();

        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.AutoLoginAttempt);
    }

    @Test
    public void attemptTokenRefresh_success_savesTokensAndEmitsNavigateToMain() {
        when(sessionRepository.hasTokens()).thenReturn(true);
        when(sessionRepository.isRememberMe()).thenReturn(true);
        when(sessionRepository.getRefreshToken()).thenReturn("refresh_token");

        LoginResult result = new LoginResult("new_access", "new_refresh", 3600);
        when(refreshTokenUseCase.execute("refresh_token"))
                .thenReturn(CompletableFuture.completedFuture(result));

        viewModel.checkAutoLogin();

        verify(sessionRepository).saveTokens("new_access", "new_refresh");
        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.NavigateToMain);
    }

    @Test
    public void attemptTokenRefresh_failure_clearsSessionAndEmitsSessionExpired() {
        when(sessionRepository.hasTokens()).thenReturn(true);
        when(sessionRepository.isRememberMe()).thenReturn(true);
        when(sessionRepository.getRefreshToken()).thenReturn("refresh_token");

        CompletableFuture<LoginResult> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Token expired"));
        when(refreshTokenUseCase.execute("refresh_token")).thenReturn(failedFuture);

        viewModel.checkAutoLogin();

        verify(sessionRepository).clearAllSessionData();
        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.SessionExpired);
    }

    @Test
    public void attemptTokenRefresh_nullRefreshToken_clearsSessionAndNavigateToWelcome() {
        when(sessionRepository.hasTokens()).thenReturn(true);
        when(sessionRepository.isRememberMe()).thenReturn(true);
        when(sessionRepository.getRefreshToken()).thenReturn(null);

        viewModel.checkAutoLogin();

        verify(sessionRepository).clearAllSessionData();
        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.NavigateToWelcome);
        assertFalse(((SplashState.NavigateToWelcome) state).sessionExpired());
    }

    @Test
    public void onSessionExpiredDisplayed_transitionsToNavigateToWelcome() {
        viewModel.onSessionExpiredDisplayed();

        SplashState state = viewModel.getState().getValue();
        assertTrue(state instanceof SplashState.NavigateToWelcome);
        assertTrue(((SplashState.NavigateToWelcome) state).sessionExpired());
    }
}
