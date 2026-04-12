package io.github.phunguy65.zms.presentation.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.auth.AuthActivity;
import io.github.phunguy65.zms.presentation.guest.JoinGuestActivity;

@AndroidEntryPoint
public class WelcomeActivity extends AppCompatActivity {

    private MaterialButton btnSignIn;
    private MaterialButton btnCreateAccount;
    private TextView tvJoinGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnSignIn = findViewById(R.id.btnSignIn);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvJoinGuest = findViewById(R.id.tvJoinGuest);
    }

    private void setupListeners() {
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, AuthActivity.class));
        });

        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, AuthActivity.class);
            intent.putExtra(AuthActivity.EXTRA_START_DESTINATION, R.id.registerFragment);
            startActivity(intent);
        });

        tvJoinGuest.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, JoinGuestActivity.class));
        });
    }
}
