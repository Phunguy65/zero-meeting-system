package io.github.phunguy65.zms.presentation.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.splash.SplashActivity;
import io.github.phunguy65.zms.presentation.videocall.VideoCallActivity;

/**
 * Single-activity host for the main app flow.
 *
 * <p>Contains a {@link NavHostFragment} that navigates between main app fragments
 * via {@code nav_graph_main.xml}. A {@link BottomNavigationView} provides tab
 * navigation between Dashboard, Calendar, and Profile screens.
 *
 * <p>The BottomNavigationView visibility is controlled by the current destination:
 * it is hidden for full-screen destinations like Schedule, CreateMeeting, JoinMeeting,
 * and Settings.
 *
 * <p>When launched from a deep link via {@link SplashActivity} with an invite token,
 * immediately hands off to {@link VideoCallActivity} and finishes.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String inviteToken = getIntent().getStringExtra(SplashActivity.EXTRA_INVITE_TOKEN);
        if (inviteToken != null && !inviteToken.isEmpty()) {
            launchVideoCallWithToken(inviteToken);
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
        if (navHostFragment == null) return;

        navController = navHostFragment.getNavController();

        // Setup BottomNavigationView with NavController
        NavigationUI.setupWithNavController(bottomNavigation, navController);

        // Control BottomNav visibility based on destination
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            updateBottomNavVisibility(destination);
        });
    }

    /**
     * Launches the video call activity with a raw invite token, then finishes this activity.
     * The VideoCallActivity will validate the token and set up the join flow.
     *
     * @param token the raw invite token from the deep link URL
     */
    private void launchVideoCallWithToken(String token) {
        Intent intent = new Intent(this, VideoCallActivity.class);
        intent.putExtra(VideoCallActivity.EXTRA_INVITE_TOKEN, token);
        intent.putExtra(VideoCallActivity.EXTRA_IS_GUEST, false);
        startActivity(intent);
        finish();
    }

    /**
     * Controls BottomNavigationView visibility based on destination.
     * Shows for tab destinations, hides for full-screen destinations.
     */
    private void updateBottomNavVisibility(NavDestination destination) {
        int destId = destination.getId();
        boolean showBottomNav = destId == R.id.dashboardFragment
                || destId == R.id.calendarFragment
                || destId == R.id.profileFragment;
        bottomNavigation.setVisibility(showBottomNav ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}
