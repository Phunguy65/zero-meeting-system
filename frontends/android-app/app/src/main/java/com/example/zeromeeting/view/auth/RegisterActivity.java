package com.example.zeromeeting.view.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
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
public class RegisterActivity extends AppCompatActivity {

    private RegisterViewModel viewModel;

    private ImageView btnBack;
    private TextInputEditText edtFullName, edtEmail, edtPassword, edtConfirmPassword;
    private CheckBox cbTerms;
    private MaterialButton btnRegisterSubmit;
    private TextView tvHaveAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo ViewModel bằng Hilt
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvHaveAccount = findViewById(R.id.tvHaveAccount);
    }

    private void setupListeners() {
        // Bấm nút Back -> đóng màn hình này lại
        btnBack.setOnClickListener(v -> finish());

        // Bấm "Sign In" -> Mở màn hình Login
        tvHaveAccount.setOnClickListener(v -> {
            // startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            // finish(); // Tùy chọn: đóng màn hình đăng ký
        });

        // Bấm nút "Create Account"
        btnRegisterSubmit.setOnClickListener(v -> {
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "Please agree to Terms and Privacy Policy", Toast.LENGTH_SHORT).show();
                return;
            }

            // Đẩy dữ liệu sang ViewModel xử lý (đúng chuẩn MVVM)
            viewModel.registerUser(fullName, email, password);
        });
    }
}
