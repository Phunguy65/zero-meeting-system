package io.github.phunguy65.zms.presentation.auth;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Single-activity host for the authentication flow.
 *
 * <p>Contains a {@link NavHostFragment} that navigates between {@code LoginFragment}
 * (start destination) and {@code RegisterFragment} via {@code nav_graph_auth.xml}.
 *
 * <p>Accepts an optional intent extra {@link #EXTRA_START_DESTINATION} to open
 * directly at the register screen when launched from {@code WelcomeActivity}.
 */
@AndroidEntryPoint
public class AuthActivity extends AppCompatActivity {

    /** Intent extra key: pass a nav destination ID to override the start destination. */
    public static final String EXTRA_START_DESTINATION = "extra_start_destination";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_auth);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        int startDest = getIntent().getIntExtra(EXTRA_START_DESTINATION, 0);
        if (startDest == R.id.registerFragment) {
            navController.navigate(R.id.registerFragment);
        }
    }
}
