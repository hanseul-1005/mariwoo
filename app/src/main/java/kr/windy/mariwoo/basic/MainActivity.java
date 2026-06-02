package kr.windy.mariwoo.basic;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.splashscreen.SplashScreen;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.helper.AlarmHelper;
import kr.windy.mariwoo.basic.helper.AlarmRestoreHelper;
import kr.windy.mariwoo.databinding.ActivityMainBinding;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 메인 Activity (Navigation Drawer 기반)
 *
 * 동작 흐름:
 *   1. SplashScreen API로 자동 로그인 동안 시스템 스플래시 유지
 *   2. 저장된 ID/PW 없음 → LoginActivity로 이동
 *   3. 저장된 ID/PW 있음 → 서버 자동 로그인
 *        - 성공: 스플래시 해제 → MainActivity 표시 → 권한 요청
 *        - 실패: LoginActivity로 이동
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "HS MainActivity";
    private static final int REQ_NOTIFICATION = 1001;

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private final OkHttpClient client = new OkHttpClient();

    // 자동 로그인 완료 여부 — false 동안 시스템 스플래시 유지
    private final AtomicBoolean isAutoLoginDone = new AtomicBoolean(false);

    // 전체화면 알림 권한 설정 화면을 열었는지 여부
    private boolean launchedSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // SplashScreen은 반드시 super.onCreate() 전에 설치
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !isAutoLoginDone.get());

        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("autoLogin", MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", "");
        String userPw = sharedPreferences.getString("user_pw", "");

        // 저장된 로그인 정보 없음 → 바로 로그인 화면으로
        if (userId.isEmpty() || userPw.isEmpty()) {
            isAutoLoginDone.set(true);
            goToLogin();
            return;
        }

        // UI 초기화 (스플래시가 가리고 있는 동안 미리 준비)
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setupUI();

        // 자동 로그인 시도
        autoLogin(userId, userPw);
    }

    // ── 자동 로그인 ──────────────────────────────────────────────

    private void autoLogin(String userId, String userPw) {
        Log.i(TAG, "자동 로그인 시도");

        String serverUrl = getString(R.string.server_user);

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
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            // 자동 로그인 성공 → 사용자 정보 저장
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

                            Log.i(TAG, "자동 로그인 성공 → 알람 복원 시작");
                            String medicineUrl = getString(R.string.server_medicine);
                            AlarmRestoreHelper.restoreAlarms(
                                    MainActivity.this, userNo, medicineUrl, () -> {
                                        // 알람 복원 완료 → 스플래시 해제 → UI 노출
                                        isAutoLoginDone.set(true);
                                        // 권한 요청 시작
                                        startPermissionFlow();
                                    }
                            );

                        } else {
                            Log.i(TAG, "자동 로그인 실패 → LoginActivity");
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

    // ── UI 초기화 ──────────────────────────────────────────────

    private void setupUI() {
        setSupportActionBar(binding.appBarMain.toolbar);

        // FAB (현재 미사용 - 향후 기능 확장용)
        binding.appBarMain.fab.setOnClickListener(view -> { });

        DrawerLayout   drawer         = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_medicine_list,
                R.id.nav_medicine_intake_list,
                R.id.nav_relation_list,
                R.id.nav_relation_intake_list,
                R.id.nav_change_info,
                R.id.nav_change_pw,
                R.id.nav_medicine_calendar)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Drawer 헤더 - 사용자 이름 표시
        View     headerView  = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navHeaderMain_textView_name);
        navUserName.setText(sharedPreferences.getString("user_name", ""));

        // 로그아웃 버튼
        Button btnLogout = headerView.findViewById(R.id.header_btn_logout);
        btnLogout.setOnClickListener(v -> {
            AlarmHelper.cancelAllAlarmsForAllMedicines(this);
            sharedPreferences.edit().clear().apply();
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 알람 알림에서 진입 시 특정 Fragment로 이동
        int navigateTo = getIntent().getIntExtra("navigate_to", -1);
        if (navigateTo != -1) {
            navController.navigate(navigateTo);
        }
    }

    // ── 권한 처리 ──────────────────────────────────────────────

    /** Step 1: 알림 권한 요청 (Android 13+) */
    private void startPermissionFlow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
                return;
            }
        }
        checkFullScreenIntentPermission();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATION) {
            checkFullScreenIntentPermission();
        }
    }

    /** Step 2: 전체화면 알림 권한 확인 (Android 14+) */
    private void checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!manager.canUseFullScreenIntent()) {
                launchedSettings = true;
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (launchedSettings) {
            launchedSettings = false;
        }
    }

    // ── 네비게이션 ──────────────────────────────────────────────

    private void goToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
