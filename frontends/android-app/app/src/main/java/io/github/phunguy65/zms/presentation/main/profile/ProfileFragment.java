package io.github.phunguy65.zms.presentation.main.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.util.InitialsDrawable;
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

    private ImageView imgAvatar;
    private TextView tvName;
    private TextView tvEmail;
    private ProgressBar progressBar;
    private LinearLayout btnAccountSettings, btnMeetingHistory, btnHelpSupport;
    private MaterialCardView cardLogOut;

    // Cache for avatar fallback
    private String currentUserId;
    private String currentFullName;

    @Nullable @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
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
        observeViewModel();
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        progressBar = view.findViewById(R.id.progressBar);
        btnAccountSettings = view.findViewById(R.id.btnAccountSettings);
        btnMeetingHistory = view.findViewById(R.id.btnMeetingHistory);
        btnHelpSupport = view.findViewById(R.id.btnHelpSupport);
        cardLogOut = view.findViewById(R.id.cardLogOut);
    }

    private void setupListeners() {
        imgAvatar.setOnClickListener(v -> navigateToAccountSettings());

        btnAccountSettings.setOnClickListener(v -> navigateToAccountSettings());

        btnMeetingHistory.setOnClickListener(
                v -> navController.navigate(R.id.action_profile_to_meetingHistory));

        btnHelpSupport.setOnClickListener(v -> {
            // TODO: Navigate to help/support
        });

        cardLogOut.setOnClickListener(v -> {
            viewModel.logOut();
            Snackbar.make(requireView(), R.string.profile_logged_out, Snackbar.LENGTH_SHORT)
                    .show();

            // Navigate to Welcome and clear back stack
            Intent intent = new Intent(requireContext(), WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void navigateToAccountSettings() {
        navController.navigate(R.id.action_profile_to_accountSettings);
    }

    private void observeViewModel() {
        viewModel.getProfileState().observe(getViewLifecycleOwner(), this::handleProfileState);
    }

    private void handleProfileState(ProfileViewModel.ProfileUiState state) {
        switch (state) {
            case ProfileViewModel.ProfileUiState.Loading loading -> showLoading();
            case ProfileViewModel.ProfileUiState.Success success -> showSuccess(success);
            case ProfileViewModel.ProfileUiState.Error error -> showError(error);
        }
    }

    private void showLoading() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        tvName.setText(R.string.profile_loading);
        tvEmail.setText("");
    }

    private void showSuccess(ProfileViewModel.ProfileUiState.Success success) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        currentUserId = success.userId();
        currentFullName = success.fullName();
        tvName.setText(success.fullName() != null ? success.fullName() : "");
        tvEmail.setText(success.email() != null ? success.email() : "");

        loadAvatar(success.avatarUrl(), success.fullName());
    }

    private void showError(ProfileViewModel.ProfileUiState.Error error) {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        tvName.setText(R.string.profile_error);
        tvEmail.setText("");

        Snackbar.make(requireView(), error.message(), Snackbar.LENGTH_LONG)
                .setAction(R.string.retry, v -> viewModel.loadProfile())
                .show();
    }

    private void loadAvatar(String avatarUrl, String fullName) {
        InitialsDrawable fallbackDrawable = new InitialsDrawable(fullName, currentUserId);

        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .transform(new CircleCrop())
                    .placeholder(fallbackDrawable)
                    .error(fallbackDrawable)
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageDrawable(fallbackDrawable);
        }
    }
}
