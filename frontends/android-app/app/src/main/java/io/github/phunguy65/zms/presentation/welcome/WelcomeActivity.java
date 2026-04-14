package io.github.phunguy65.zms.presentation.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.auth.AuthActivity;
import io.github.phunguy65.zms.presentation.common.LanguagePickerSheet;
import io.github.phunguy65.zms.presentation.videocall.VideoCallActivity;

@AndroidEntryPoint
public class WelcomeActivity extends AppCompatActivity {

    private MaterialButton btnLanguage;
    private MaterialButton btnSignIn;
    private MaterialButton btnCreateAccount;
    private TextView tvJoinGuest;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initViews();
        setupListeners();
        updateLanguageButton();
    }

    private void initViews() {
        btnLanguage = findViewById(R.id.btnLanguage);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvJoinGuest = findViewById(R.id.tvJoinGuest);
    }

    private void setupListeners() {
        btnLanguage.setOnClickListener(v -> LanguagePickerSheet.show(getSupportFragmentManager()));

        btnSignIn.setOnClickListener(
                v -> startActivity(new Intent(WelcomeActivity.this, AuthActivity.class)));

        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, AuthActivity.class);
            intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, R.id.registerFragment);
            startActivity(intent);
        });

        tvJoinGuest.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, VideoCallActivity.class);
            intent.putExtra(VideoCallActivity.EXTRA_IS_GUEST, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
    }

    /**
     * Updates the language button text to show current language code (EN/VI).
     * Also sets accessibility content description.
     */
    private void updateLanguageButton() {
        String langTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        String code;
        String displayName;

        if (langTag.startsWith("vi")) {
            code = "VI";
            displayName = getString(R.string.language_vietnamese_native);
        } else {
            code = "EN";
            displayName = getString(R.string.language_english_native);
        }

        btnLanguage.setText(code);
        btnLanguage.setContentDescription(getString(R.string.cd_language_button, displayName));
    }
}
