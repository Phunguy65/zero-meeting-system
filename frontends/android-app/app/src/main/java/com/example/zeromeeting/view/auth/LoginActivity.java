package com.example.zeromeeting.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLoginSubmit, btnGoogle, btnApple;
    private TextView tvNeedAccount, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnApple = findViewById(R.id.btnApple);
        tvNeedAccount = findViewById(R.id.tvNeedAccount);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
    }

    private void setupListeners() {
        // Đóng màn hình
        btnBack.setOnClickListener(v -> finish());

        // Chuyển sang màn hình Đăng ký
        tvNeedAccount.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // finish(); // Bỏ comment nếu muốn đóng màn login khi sang đăng ký
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        // Xử lý nút Đăng nhập chính
        btnLoginSubmit.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            viewModel.loginUser(email, password);
            Toast.makeText(this, "Đang xử lý đăng nhập...", Toast.LENGTH_SHORT).show();
        });
    }
}
