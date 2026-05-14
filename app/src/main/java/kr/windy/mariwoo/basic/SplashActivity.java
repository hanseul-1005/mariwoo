package kr.windy.mariwoo.basic;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
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
 *   1. 권한 요청 (알림 → 전체화면 알림)
 *   2. 권한 처리 완료 후 자동 로그인 체크
 *        - 저장된 ID/PW 있음 → 로그인 시도
 *            - 성공: MainActivity로 이동
 *            - 실패: LoginActivity로 이동
 *        - 저장된 ID/PW 없음 → 2초 후 LoginActivity로 이동
 *
 * AndroidManifest.xml에서 launcher Activity로 설정됨
 */
public class SplashActivity extends AppCompatActivity {

    private static final int REQ_NOTIFICATION = 1001;

    private SharedPreferences sharedPreferences;
    private String userId    = "";
    private String userPw    = "";
    private String serverUrl = "";

    /** 전체화면 알림 권한 설정 화면을 열었는지 여부 (onResume에서 재진입 감지용) */
    private boolean launchedSettings = false;

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

        // 권한 요청부터 시작
        startPermissionFlow();
    }

    // ──────────────────────────────────────────────
    // 권한 처리 흐름
    // ──────────────────────────────────────────────

    /**
     * Step 1: 알림 권한 요청 (Android 13+)
     * 이미 허용됐거나 미만 버전이면 바로 Step 2로 진행
     */
    private void startPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
                return; // onRequestPermissionsResult에서 Step 2 진행
            }
        }
        checkFullScreenIntentPermission();
    }

    /** 알림 권한 결과 수신 → Step 2로 진행 */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION) {
            checkFullScreenIntentPermission();
        }
    }

    /**
     * Step 2: 전체화면 알림 권한 확인 (Android 14+)
     * 권한 없으면 시스템 설정 화면으로 이동, 돌아오면 onResume에서 Step 3 진행
     * 이미 허용됐거나 미만 버전이면 바로 Step 3으로 진행
     */
    private void checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.canUseFullScreenIntent()) {
                launchedSettings = true;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return; // onResume에서 Step 3 진행
            }
        }
        startAutoLoginFlow();
    }

    /** 설정 화면에서 돌아왔을 때 Step 3 진행 */
    @Override
    protected void onResume() {
        super.onResume();
        if (launchedSettings) {
            launchedSettings = false;
            startAutoLoginFlow();
        }
    }

    // ──────────────────────────────────────────────
    // Step 3: 자동 로그인 처리
    // ──────────────────────────────────────────────

    /**
     * 저장된 ID/PW로 자동 로그인 시도
     * 없으면 2초 후 로그인 화면으로 이동
     */
    private void startAutoLoginFlow() {
        if (!"".equals(userId) && !"".equals(userPw)) {
            login();
        } else {
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
