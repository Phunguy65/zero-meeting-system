package io.github.phunguy65.zms.presentation.main.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.common.LanguagePickerSheet;

/**
 * Fragment for app settings.
 * Allows users to change language and view about information.
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private ImageView btnBack;
    private LinearLayout rowLanguage;
    private LinearLayout rowAbout;
    private TextView tvCurrentLanguage;

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

        initViews(view);
        setupListeners();
        updateLanguageDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLanguageDisplay();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        rowLanguage = view.findViewById(R.id.rowLanguage);
        rowAbout = view.findViewById(R.id.rowAbout);
        tvCurrentLanguage = view.findViewById(R.id.tvCurrentLanguage);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v ->
                Navigation.findNavController(v).popBackStack());

        rowLanguage.setOnClickListener(v -> {
            LanguagePickerSheet.show(getChildFragmentManager());
        });

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
    }
}
