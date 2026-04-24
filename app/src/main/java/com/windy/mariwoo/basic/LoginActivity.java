package com.windy.mariwoo.basic;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

public class LoginActivity extends AppCompatActivity {

    private EditText editId;
    private EditText editPw;
    private AppCompatButton btnLogin;
    private AppCompatButton btnSign;

    private SharedPreferences sharedPreferences;
    private String userId = "";
    private String userPw = "";
    private String serverUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        serverUrl = getString(R.string.server_user);

        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userId = sharedPreferences.getString("user_id", "");
        userPw = sharedPreferences.getString("user_pw", "");

        editId = findViewById(R.id.loginActivity_editText_id);
        editPw = findViewById(R.id.loginActivity_editText_pw);

        if(!"".equals(userId) && !"".equals(userPw)) {
            login();
        }

        btnSign = findViewById(R.id.loginActivity_button_sign);
        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, SignActivity.class);
                startActivity(intent);
            }
        });

        btnLogin = findViewById(R.id.loginActivity_button_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userId = editId.getText().toString().trim();
                userPw = editPw.getText().toString().trim();

                login();

            }
        });
    }

    private void login() {
        Log.i("HS LoginActivity", "login start");



        // POST 파라미터 추가
        RequestBody formBody = new FormBody  .Builder()
                .add("cmd", "login")
                .add("id", userId)
                .add("pw", userPw)
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS LoginActivity", "request : "+request.toString());
        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                // 서브 스레드 Ui 변경 할 경우 에러
                // 메인스레드 Ui 설정
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "응답 성공");
                            final String responseData = response.body().string();

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {
                                String userNo = json.getString("no");
                                String userName = json.getString("name");
                                String userEmail = json.getString("email");
                                String userTel = json.getString("tel");
                                String userBirth = json.getString("birth");

                                Log.i("HS LoginActivity", "userNo : "+userNo);
                                Log.i("HS LoginActivity", "userName : "+userName);

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_no", userNo);
                                editor.putString("user_id", userId);
                                editor.putString("user_pw", userPw);
                                editor.putString("user_name", userName);
                                editor.putString("user_email", userEmail);
                                editor.putString("user_tel", userTel);
                                editor.putString("user_birth", userBirth);

                                editor.apply();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);

                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요." + responseData, Toast.LENGTH_SHORT).show();
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