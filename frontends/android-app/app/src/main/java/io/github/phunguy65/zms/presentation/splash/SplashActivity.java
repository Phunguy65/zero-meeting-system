package io.github.phunguy65.zms.presentation.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import dagger.hilt.android.AndroidEntryPoint;
import io.github.phunguy65.zms.frontends.R;
import io.github.phunguy65.zms.presentation.welcome.WelcomeActivity;

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
