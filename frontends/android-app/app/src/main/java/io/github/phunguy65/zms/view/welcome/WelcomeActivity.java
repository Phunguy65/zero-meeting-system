package io.github.phunguy65.zms.view.welcome;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.view.auth.login.LoginActivity;
import io.github.phunguy65.zms.view.auth.register.RegisterActivity;
import io.github.phunguy65.zms.view.guest.JoinGuestActivity;

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
        // Xử lý sự kiện bấm nút Sign In
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        // Xử lý sự kiện bấm nút Create Account
        btnCreateAccount.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });

        // Xử lý sự kiện bấm chữ Join as Guest
        tvJoinGuest.setOnClickListener(v -> {
            // Xử lý logic vào thẳng phòng họp mà không cần đăng nhập
            startActivity(new Intent(WelcomeActivity.this, JoinGuestActivity.class));
        });
    }
}
