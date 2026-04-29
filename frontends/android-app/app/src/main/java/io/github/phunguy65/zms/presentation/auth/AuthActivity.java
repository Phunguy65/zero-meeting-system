package io.github.phunguy65.zms.presentation.auth;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Single-activity host for the authentication flow.
 *
 * <p>Contains a {@link NavHostFragment} that navigates between {@code LoginFragment}
 * and {@code RegisterFragment} via {@code nav_graph_auth.xml}.
 *
 * <p>Accepts an optional intent extra {@link #EXTRA_START_DESTINATION} to open
 * directly at the register screen when launched from {@code WelcomeActivity}.
 * The start destination is set dynamically to ensure correct back stack behavior:
 * pressing back from Register will exit to Welcome (not go to Login).
 */
@AndroidEntryPoint
public class AuthActivity extends AppCompatActivity {

    /** Intent extra key: pass a nav destination ID to override the start destination. */
    public static final String EXTRA_START_DESTINATION = "extra_start_destination";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply fade transition if this is a locale-change recreation
        if (savedInstanceState != null) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_auth);
        if (navHostFragment == null) return;

        NavController navController = navHostFragment.getNavController();

        // Only set dynamic start destination on fresh launch (not on config change)
        if (savedInstanceState == null) {
            int startDest = getIntent().getIntExtra(EXTRA_START_DESTINATION, 0);
            if (startDest == R.id.registerFragment) {
                // Dynamically set start destination to Register so back stack is correct
                NavGraph navGraph =
                        navController.getNavInflater().inflate(R.navigation.nav_graph_auth);
                navGraph.setStartDestination(R.id.registerFragment);
                navController.setGraph(navGraph, getIntent().getExtras());
            }
            // If startDest is not register, default graph (login as start) is already set via XML
        }
    }
}
