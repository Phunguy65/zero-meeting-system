package com.example.zeromeeting.view.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper; // Import thêm thư viện này

import androidx.appcompat.app.AppCompatActivity;

import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
// Import thêm đường dẫn tới WelcomeActivity của bạn
import com.example.zeromeeting.view.welcome.WelcomeActivity;
import com.example.zeromeeting.view.auth.RegisterActivity;
import com.example.zeromeeting.view.auth.LoginActivity;
import com.example.zeromeeting.view.guest.JoinGuestActivity;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Tạo độ trễ 2 giây (2000 mili-giây) rồi chuyển màn hình
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Tạo lệnh di chuyển từ Splash sang Welcome
                Intent intent = new Intent(SplashActivity.this, JoinGuestActivity.class);
                startActivity(intent);

                // Đóng SplashActivity lại để khi người dùng bấm nút Back ở màn Welcome sẽ thoát app luôn, không quay lại màn hình trắng này nữa
                finish();
            }
        }, 2000);
    }
}
