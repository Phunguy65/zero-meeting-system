package com.example.zeromeeting.view.welcome;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R; // Import file R của bạn

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
            // startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        });

        // Xử lý sự kiện bấm nút Create Account
        btnCreateAccount.setOnClickListener(v -> {
            // startActivity(new Intent(WelcomeActivity.this, RegisterActivity.class));
        });

        // Xử lý sự kiện bấm chữ Join as Guest
        tvJoinGuest.setOnClickListener(v -> {
            // Xử lý logic vào thẳng phòng họp mà không cần đăng nhập
            // startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
        });
    }
}
