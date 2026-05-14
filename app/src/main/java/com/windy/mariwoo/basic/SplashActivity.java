package com.windy.mariwoo.basic;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 스플래시 Activity
 *
 * 동작 흐름:
 *   1. SharedPreferences에서 저장된 ID/PW 확인
 *   2. 저장된 정보 있음 → 자동 로그인 시도
 *        - 성공: MainActivity로 이동
 *        - 실패: LoginActivity로 이동
 *   3. 저장된 정보 없음 → 2초 후 LoginActivity로 이동
 *
 * AndroidManifest.xml에서 launcher Activity로 설정됨
 */
public class SplashActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private String userId    = "";
    private String userPw    = "";
    private String serverUrl = "";

    private final OkHttpClient client = new OkHttpClient();

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

        serverUrl = getString(R.string.server_user);
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userId = sharedPreferences.getString("user_id", "");
        userPw = sharedPreferences.getString("user_pw", "");

        if (!"".equals(userId) && !"".equals(userPw)) {
            // 저장된 정보 있음 → 자동 로그인 시도 (네트워크 응답 후 화면 전환)
            login();
        } else {
            // 저장된 정보 없음 → 2초 후 로그인 화면으로 이동
            new Handler().postDelayed(this::goToLogin, 2000);
        }
    }

    /**
     * 서버에 자동 로그인 요청
     * - 성공: 사용자 정보를 SharedPreferences에 저장 후 MainActivity로 이동
     * - 실패 / 오류: LoginActivity로 이동
     */
    private void login() {
        Log.i("HS SplashActivity", "자동 로그인 시도");

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "login")
                .add("id",  userId)
                .add("pw",  userPw)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> goToLogin());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    runOnUiThread(() -> goToLogin());
                    return;
                }
                final String responseData = response.body().string();

                runOnUiThread(() -> {
                    try {
                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            // 자동 로그인 성공 → 사용자 정보 저장 후 MainActivity로
                            String userNo    = json.getString("no");
                            String userName  = json.getString("name");
                            String userEmail = json.getString("email");
                            String userTel   = json.getString("tel");
                            String userBirth = json.getString("birth");

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("user_no",    userNo);
                            editor.putString("user_id",    userId);
                            editor.putString("user_pw",    userPw);
                            editor.putString("user_name",  userName);
                            editor.putString("user_email", userEmail);
                            editor.putString("user_tel",   userTel);
                            editor.putString("user_birth", userBirth);
                            editor.apply();

                            Log.i("HS SplashActivity", "자동 로그인 성공 → MainActivity");
                            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                        } else {
                            // 자동 로그인 실패 → 로그인 화면으로
                            Log.i("HS SplashActivity", "자동 로그인 실패 → LoginActivity");
                            goToLogin();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        goToLogin();
                    }
                });
            }
        });
    }

    /** LoginActivity로 이동 후 현재 Activity 종료 */
    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
