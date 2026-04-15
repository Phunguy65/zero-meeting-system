package io.github.phunguy65.zms.presentation.main.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.domain.model.Theme;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.LanguagePickerSheet;

/**
 * Fragment for app settings.
 * Allows users to change language, theme, and view about information.
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private SettingsViewModel viewModel;

    private View btnBackWrapper;
    private LinearLayout rowLanguage;
    private LinearLayout rowTheme;
    private LinearLayout rowAbout;
    private TextView tvCurrentLanguage;
    private TextView tvCurrentTheme;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        initViews(view);
        setupListeners();
        updateLanguageDisplay();
        updateThemeDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLanguageDisplay();
        updateThemeDisplay();
    }

    private void initViews(View view) {
        btnBackWrapper = view.findViewById(R.id.btnBackWrapper);
        rowLanguage = view.findViewById(R.id.rowLanguage);
        rowTheme = view.findViewById(R.id.rowTheme);
        rowAbout = view.findViewById(R.id.rowAbout);
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);
    }

    private void setupListeners() {
        btnBackWrapper.setOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        rowLanguage.setOnClickListener(v -> {
            LanguagePickerSheet.show(getChildFragmentManager());
        });

        rowTheme.setOnClickListener(v -> showThemeDialog());

        rowAbout.setOnClickListener(v -> {
            // TODO: Navigate to About screen or show about dialog
        });
    }

    private void updateLanguageDisplay() {
        String langTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String displayName;

        if (langTag.startsWith("vi")) {
            displayName = getString(R.string.language_vietnamese_native);
        } else {
            displayName = getString(R.string.language_english_native);
        }

        tvCurrentLanguage.setText(displayName);
        // Update accessibility content description
        rowLanguage.setContentDescription(
                getString(R.string.settings_language) + ", " + displayName + ", " +
                getString(R.string.cd_activate_to_change));
    }

    private void updateThemeDisplay() {
        Theme currentTheme = viewModel.getTheme();
        String displayName = getThemeDisplayName(currentTheme);
        tvCurrentTheme.setText(displayName);
        rowTheme.setContentDescription(
                getString(R.string.settings_theme) + ", " + displayName + ", " +
                getString(R.string.cd_activate_to_change));
    }

    private String getThemeDisplayName(Theme theme) {
        return switch (theme) {
            case DARK -> getString(R.string.theme_dark);
            case LIGHT -> getString(R.string.theme_light);
            case SYSTEM -> getString(R.string.theme_system);
        };
    }

    private void showThemeDialog() {
        Theme currentTheme = viewModel.getTheme();

        String[] themeOptions = {
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.theme_system)
        };

        Theme[] themes = {Theme.LIGHT, Theme.DARK, Theme.SYSTEM};

        int checkedItem = switch (currentTheme) {
            case LIGHT -> 0;
            case DARK -> 1;
            case SYSTEM -> 2;
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme_dialog_title)
                .setSingleChoiceItems(themeOptions, checkedItem, (dialog, which) -> {
                    Theme selectedTheme = themes[which];
                    applyTheme(selectedTheme);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void applyTheme(Theme theme) {
        viewModel.setTheme(theme);

        int nightMode = switch (theme) {
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        };
        AppCompatDelegate.setDefaultNightMode(nightMode);

        requireActivity().recreate();
    }
}
