package com.windy.mariwoo.basic;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.user.SignActivity;

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
 * 로그인 Activity
 *
 * 동작 흐름:
 *   1. onCreate → SharedPreferences에 저장된 ID/PW 확인
 *   2. 저장된 정보가 있으면 자동 로그인 (login() 즉시 호출)
 *   3. 로그인 성공 → MainActivity로 이동, SharedPreferences에 사용자 정보 저장
 *   4. 로그인 실패 → 안내 토스트 표시
 *
 * 자동 로그인:
 *   - SharedPreferences "autoLogin" 키: user_id, user_pw
 *   - 둘 다 비어 있지 않으면 자동으로 login() 호출
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editId;       // 아이디 입력
    private EditText editPw;       // 비밀번호 입력
    private AppCompatButton btnLogin; // 로그인 버튼
    private AppCompatButton btnSign;  // 회원가입 버튼

    private SharedPreferences sharedPreferences;
    private String userId    = ""; // 현재 입력된 아이디 (또는 저장된 아이디)
    private String userPw    = ""; // 현재 입력된 비밀번호 (또는 저장된 비밀번호)
    private String serverUrl = "";

    // 싱글톤 OkHttpClient (매 요청마다 생성 방지)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // 루트: systemBars 패딩만 적용
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 스크롤 뷰: IME inset만큼 하단 패딩 → 키보드 등장 시 스크롤 영역 축소
        // (SignActivity와 동일한 방식)
        NestedScrollView scrollView = findViewById(R.id.loginActivity_scrollView);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int imeExtra = Math.max(0, imeInsets.bottom - systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeExtra);
            return insets;
        });

        // 포커스된 EditText가 키보드 바로 위에 오도록 자동 스크롤 (SignActivity와 동일)
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View focused = scrollView.findFocus();
            if (focused != null) {
                Rect rect = new Rect();
                focused.getDrawingRect(rect);
                scrollView.requestChildRectangleOnScreen(focused, rect, false);
            }
        });

        // 다크 모드 비활성화
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        serverUrl = getString(R.string.server_user);

        // 자동 로그인용 SharedPreferences 로드
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userId = sharedPreferences.getString("user_id", "");
        userPw = sharedPreferences.getString("user_pw", "");

        editId = findViewById(R.id.loginActivity_editText_id);
        editPw = findViewById(R.id.loginActivity_editText_pw);

        // 저장된 ID/PW가 있으면 자동 로그인 시도
        if (!"".equals(userId) && !"".equals(userPw)) {
            login();
        }

        // 회원가입 화면 이동
        btnSign = findViewById(R.id.loginActivity_button_sign);
        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignActivity.class);
                startActivity(intent);
            }
        });

        // 로그인 버튼 → 입력값으로 login() 호출
        btnLogin = findViewById(R.id.loginActivity_button_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userId = editId.getText().toString().trim();
                userPw = editPw.getText().toString().trim();
                login();
            }
        });

        // 비밀번호 칸에서 완료(Done) 버튼 누르면 로그인 버튼 액션 실행
        editPw.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick();
                return true;
            }
            return false;
        });
    }

    /**
     * 서버에 로그인 요청
     * - cmd: "login", id, pw 전송
     * - 성공 시: 사용자 정보를 SharedPreferences에 저장하고 MainActivity로 이동
     * - 실패 시: 안내 토스트 표시
     *
     * 서버 응답 JSON:
     *   성공: {"result":"true", "no":"...", "name":"...", "email":"...", "tel":"...", "birth":"..."}
     *   실패: {"result":"false"}
     */
    private void login() {
        Log.i("HS LoginActivity", "login start");

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "login")
                .add("id",  userId)
                .add("pw",  userPw)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS LoginActivity", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // response.body().string()은 IO 작업이므로 runOnUiThread 밖에서 호출
                if (response.body() == null) return;
                final String responseData = response.body().string();

                // UI 업데이트는 메인 스레드에서
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("HS", "응답 성공");

                            JSONObject json   = new JSONObject(responseData);
                            String     result = json.getString("result");

                            if ("true".equals(result)) {
                                // 로그인 성공 → 사용자 정보 저장
                                String userNo    = json.getString("no");
                                String userName  = json.getString("name");
                                String userEmail = json.getString("email");
                                String userTel   = json.getString("tel");
                                String userBirth = json.getString("birth");

                                Log.i("HS LoginActivity", "userNo : "   + userNo);
                                Log.i("HS LoginActivity", "userName : " + userName);

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_no",    userNo);
                                editor.putString("user_id",    userId);
                                editor.putString("user_pw",    userPw);
                                editor.putString("user_name",  userName);
                                editor.putString("user_email", userEmail);
                                editor.putString("user_tel",   userTel);
                                editor.putString("user_birth", userBirth);
                                editor.apply();

                                // 메인 화면으로 이동 후 현재 Activity 종료
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();

                            } else {
                                // 로그인 실패
                                Toast.makeText(getApplicationContext(),
                                        "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요.",
                                        Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }
}
