package io.github.phunguy65.zms.presentation.splash;

/**
 * Sealed interface representing the states of the splash screen during auto-login flow.
 *
 * <p>State transitions:
 * <pre>
 * Loading → (tokens exist + rememberMe) → AutoLoginAttempt → Success → NavigateToMain
 *                                                          → Failure → SessionExpired → NavigateToWelcome
 * Loading → (no tokens or rememberMe=false) → NavigateToWelcome
 * </pre>
 */
public sealed interface SplashState
        permits SplashState.Loading,
                SplashState.AutoLoginAttempt,
                SplashState.SessionExpired,
                SplashState.NavigateToWelcome,
                SplashState.NavigateToMain {

    /** Initial state while checking auto-login eligibility. */
    record Loading() implements SplashState {}

    /** State when attempting to refresh tokens for auto-login. */
    record AutoLoginAttempt() implements SplashState {}

    /** State when auto-login failed (token refresh failed). Shows message briefly. */
    record SessionExpired() implements SplashState {}

    /** State to navigate to Welcome screen (no saved session or after SessionExpired). */
    record NavigateToWelcome(boolean sessionExpired) implements SplashState {
        /** Navigate to Welcome without session expired flag. */
        public NavigateToWelcome() {
            this(false);
        }
    }

    /** State to navigate to Main screen (auto-login successful). */
    record NavigateToMain() implements SplashState {}
}
