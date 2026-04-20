package io.github.phunguy65.zms.presentation.videocall;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

/**
 * Separate Activity for video call flow.
 * Runs as a separate Android task for call isolation and PiP support.
 *
 * Entry points:
 * - MainActivity → "Join Meeting" → VideoCallActivity with isGuest=false
 * - WelcomeActivity → "Join as Guest" → VideoCallActivity with isGuest=true
 */
@AndroidEntryPoint
public class VideoCallActivity extends AppCompatActivity {

    public static final String EXTRA_IS_GUEST = "isGuest";
    public static final String EXTRA_MEETING_CODE = "meetingCode";
    public static final String EXTRA_MEETING_ID = "meetingId";

    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        setupNavigation();
        initializeViewModelFromIntent();
    }

    /**
     * Pushes intent extras into CallViewModel so fragments can read from ViewModel
     * instead of casting to this Activity. This follows Dependency Inversion Principle.
     */
    private void initializeViewModelFromIntent() {
        CallViewModel viewModel = new ViewModelProvider(this).get(CallViewModel.class);
        viewModel.setIsGuest(getIntent().getBooleanExtra(EXTRA_IS_GUEST, false));

        String meetingCode = getIntent().getStringExtra(EXTRA_MEETING_CODE);
        if (meetingCode != null && !meetingCode.isEmpty()) {
            viewModel.setMeetingCode(meetingCode);
        }

        String meetingId = getIntent().getStringExtra(EXTRA_MEETING_ID);
        if (meetingId != null && !meetingId.isEmpty()) {
            viewModel.setMeetingUuid(meetingId);
        }

        if (!viewModel.isGuestValue()) {
            viewModel.loadUserDisplayName();
        }
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_video_call);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }
    }

    /**
     * Returns whether the current session is a guest session.
     */
    public boolean isGuestSession() {
        return getIntent().getBooleanExtra(EXTRA_IS_GUEST, false);
    }

    /**
     * Returns the pre-filled meeting code if provided via intent.
     */
    @Nullable public String getMeetingCode() {
        return getIntent().getStringExtra(EXTRA_MEETING_CODE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
