package io.github.phunguy65.zms.presentation.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.welcome.WelcomeActivity;

/**
 * Profile fragment displaying user info and settings menu.
 *
 * <p>Shows user avatar, name, email, and menu options for account settings,
 * meeting history, help, and logout.
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;
    private NavController navController;

    private LinearLayout btnAccountSettings, btnMeetingHistory, btnHelpSupport;
    private MaterialCardView cardLogOut;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        navController = NavHostFragment.findNavController(this);

        initViews(view);
        setupListeners();
    }

    private void initViews(View view) {
        btnAccountSettings = view.findViewById(R.id.btnAccountSettings);
        btnMeetingHistory = view.findViewById(R.id.btnMeetingHistory);
        btnHelpSupport = view.findViewById(R.id.btnHelpSupport);
        cardLogOut = view.findViewById(R.id.cardLogOut);
    }

    private void setupListeners() {
        btnAccountSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_profile_to_settings);
        });

        btnMeetingHistory.setOnClickListener(v -> {
            // TODO: Navigate to meeting history
        });

        btnHelpSupport.setOnClickListener(v -> {
            // TODO: Navigate to help/support
        });

        cardLogOut.setOnClickListener(v -> {
            viewModel.logOut();
            Snackbar.make(requireView(), R.string.profile_logged_out, Snackbar.LENGTH_SHORT).show();

            // Navigate to Welcome and clear back stack
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
