package com.windy.mariwoo.basic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;

/**
 * 스플래시 Activity
 *
 * 역할:
 *   - 앱 최초 진입 시 로고/로딩 화면을 2초간 표시
 *   - 이후 LoginActivity로 자동 이동
 *
 * AndroidManifest.xml에서 launcher Activity로 설정됨
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        // 시스템 바 패딩 처리 (edge-to-edge 레이아웃)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2초 후 LoginActivity로 이동 (스플래시 화면 유지 시간)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d("HS", "SplashActivity → LoginActivity");
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // 스플래시 화면을 백스택에서 제거
            }
        }, 2000);
    }
}
