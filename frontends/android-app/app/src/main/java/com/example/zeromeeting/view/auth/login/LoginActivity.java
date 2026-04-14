package com.example.zeromeeting.view.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.zeromeeting.core.utils.TokenManager;
import com.example.zeromeeting.view.auth.register.RegisterActivity;
import com.example.zeromeeting.view.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    @Inject
    public TokenManager tokenManager;
    private LoginViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLoginSubmit, btnGoogle, btnApple;
    private TextView tvNeedAccount, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Nhớ thay R.layout.activity_login bằng tên file XML màn hình login thực tế của bạn nếu khác nhé
        setContentView(R.layout.activity_login);

        // 1. Ánh xạ View và thiết lập sự kiện
        initViews();

        // 2. Khởi tạo ViewModel bằng Hilt
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // 3. Gắn sự kiện click
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
            finish();
        });

        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show();
        });

        // Xử lý nút Đăng nhập chính (Gọi API thật)
        btnLoginSubmit.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Gọi hàm từ ViewModel và Observe kết quả
            viewModel.login(email, password).observe(this, resource -> {
                switch (resource.status) {
                    case LOADING:
                        btnLoginSubmit.setEnabled(false);
                        btnLoginSubmit.setText("Đang đăng nhập...");
                        break;

                    case SUCCESS:
                        btnLoginSubmit.setEnabled(true);
                        btnLoginSubmit.setText("Login");

                        // Lưu token vào bộ nhớ máy
                        String accessToken = resource.data.getAccessToken();
                        String refreshToken = resource.data.getRefreshToken();
                        tokenManager.saveTokens(accessToken, refreshToken); // <-- THÊM DÒNG NÀY

                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        break;

                    case ERROR:
                        btnLoginSubmit.setEnabled(true);
                        btnLoginSubmit.setText("Login");
                        Toast.makeText(this, "Lỗi: " + resource.message, Toast.LENGTH_LONG).show();
                        break;
                }
            });
        });
    }
}
