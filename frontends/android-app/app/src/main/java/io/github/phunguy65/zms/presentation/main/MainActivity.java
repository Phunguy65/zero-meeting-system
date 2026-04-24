package io.github.phunguy65.zms.presentation.main;

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
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
