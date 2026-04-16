package io.github.phunguy65.zms.presentation.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.main.MainActivity;
import io.github.phunguy65.zms.presentation.welcome.WelcomeActivity;

/**
 * Splash screen activity handling the auto-login flow.
 *
 * <p>Displays splash branding for 2 seconds, then checks auto-login eligibility:
 * <ul>
 *   <li>If eligible, attempts token refresh</li>
 *   <li>On success, navigates to MainActivity</li>
 *   <li>On failure, shows "Session expired" message briefly, then navigates to WelcomeActivity</li>
 *   <li>If not eligible, navigates directly to WelcomeActivity</li>
 * </ul>
 */
@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    /** Extra key for passing session expired flag to WelcomeActivity. */
    public static final String EXTRA_SESSION_EXPIRED = "session_expired";

    private static final long SPLASH_DELAY_MS = 2000;
    private static final long SESSION_EXPIRED_DISPLAY_MS = 1500;

    private SplashViewModel viewModel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Views
    private TextView tvAppName;
    private LinearLayout sessionExpiredContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        viewModel = new ViewModelProvider(this).get(SplashViewModel.class);

        initViews();
        observeState();

        handler.postDelayed(() -> viewModel.checkAutoLogin(), SPLASH_DELAY_MS);
    }

    private void initViews() {
        tvAppName = findViewById(R.id.tvAppName);
        sessionExpiredContainer = findViewById(R.id.sessionExpiredContainer);
    }

    private void observeState() {
        viewModel.getState().observe(this, state -> {
            switch (state) {
                case SplashState.Loading ignored -> {
                    showLoadingState();
                }
                case SplashState.AutoLoginAttempt ignored -> {
                    showLoadingState();
                }
                case SplashState.SessionExpired ignored -> {
                    showSessionExpiredState();
                    handler.postDelayed(
                            () -> viewModel.onSessionExpiredDisplayed(),
                            SESSION_EXPIRED_DISPLAY_MS);
                }
                case SplashState.NavigateToWelcome nav -> navigateToWelcome(nav.sessionExpired());
                case SplashState.NavigateToMain ignored -> navigateToMain();
            }
        });
    }

    private void showLoadingState() {
        tvAppName.setVisibility(View.VISIBLE);
        if (sessionExpiredContainer != null) {
            sessionExpiredContainer.setVisibility(View.GONE);
        }
    }

    private void showSessionExpiredState() {
        if (sessionExpiredContainer != null) {
            sessionExpiredContainer.setVisibility(View.VISIBLE);
            sessionExpiredContainer.setAlpha(0f);
            sessionExpiredContainer.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void navigateToWelcome(boolean sessionExpired) {
        Intent intent = new Intent(this, WelcomeActivity.class);
        if (sessionExpired) {
            intent.putExtra(EXTRA_SESSION_EXPIRED, true);
        }
        startActivity(intent);
        finish();
        applyTransitionIfEnabled();
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        applyTransitionIfEnabled();
    }

    /**
     * Applies activity transition only if system animations are enabled.
     * Respects the "Remove animations" accessibility setting.
     */
    private void applyTransitionIfEnabled() {
        float scale = Settings.Global.getFloat(
                getContentResolver(), Settings.Global.TRANSITION_ANIMATION_SCALE, 1f);
        if (scale > 0) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
