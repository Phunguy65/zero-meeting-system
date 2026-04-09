package com.example.zeromeeting.view.splash;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.example.zeromeeting.view.welcome.WelcomeActivity;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper())
                .postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                // Tạo lệnh di chuyển từ Splash sang Welcome
                                Intent intent =
                                        new Intent(SplashActivity.this, WelcomeActivity.class);
                                startActivity(intent);

                                // Đóng SplashActivity lại để khi người dùng bấm nút Back ở màn
                                // Welcome sẽ thoát app luôn, không quay lại màn hình trắng này nữa
                                finish();
                            }
                        },
                        2000);
    }
}
