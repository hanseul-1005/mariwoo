package com.windy.mariwoo.basic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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

public class SignActivity extends AppCompatActivity {

    private ImageView imgBack;

    private CheckBox chkUse;
    private CheckBox chkPrivacy;
    private CheckBox chkMarketing;

    private TextView tvUse;
    private TextView tvPrivacy;
    private TextView tvMarketing;

    private EditText editId;
    private EditText editPw;
    private EditText editPwCheck;
    private TextView tvPwCheck;
    private EditText editTel;
    private EditText editEmail;
    private EditText editEmailAddr;
    private EditText editBirthYear;
    private EditText editBirthMonth;
    private EditText editBirthDay;
    private Button btnSign;

    private String serverUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        serverUrl = getString(R.string.server_user);

        editId = findViewById(R.id.signActivity_editText_id);
        editPw = findViewById(R.id.signActivity_editText_pw);
        editPwCheck = findViewById(R.id.signActivity_editText_chk_pw);
        tvPwCheck = findViewById(R.id.signActivity_textView_check);
        editTel = findViewById(R.id.signActivity_editText_tel);
        editEmail = findViewById(R.id.signActivity_editText_email);
        editEmailAddr = findViewById(R.id.signActivity_editText_emailAddr);
        editBirthYear = findViewById(R.id.signActivity_editText_birth_year);
        editBirthMonth = findViewById(R.id.signActivity_editText_birth_month);
        editBirthDay = findViewById(R.id.signActivity_editText_birth_day);

        String pw = editPw.getText().toString();
        String pwCheck = editPwCheck.getText().toString();

        editPwCheck.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!pw.equals(pwCheck)) {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");
                        }
                        else {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                        }
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        imgBack = findViewById(R.id.signActivity_imageView_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvUse = findViewById(R.id.signActivity_textView_use);
        tvUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "use");
                startActivity(intent);
            }
        });

        tvPrivacy = findViewById(R.id.signActivity_textView_privacy);
        tvPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "privacy");
                startActivity(intent);
            }
        });

        tvMarketing = findViewById(R.id.signActivity_textView_marketing);
        tvMarketing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "marketing");
                startActivity(intent);
            }
        });

        btnSign = findViewById(R.id.signActivity_button_sign);
        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String id = editId.getText().toString();
                String pw = editPw.getText().toString();
                String tel = editTel.getText().toString();
                String email = editEmail.getText().toString();
                String birthYear = editBirthYear.getText().toString();
                String birthMonth = editBirthMonth.getText().toString();
                String birthDay = editBirthDay.getText().toString();

                if("".equals(id)) {
                    Toast.makeText(getApplicationContext(), "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(pw)) {
                    Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(tel)) {
                    Toast.makeText(getApplicationContext(), "연락처를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(email)) {
                    Toast.makeText(getApplicationContext(), "이메일를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(birthYear)) {
                    Toast.makeText(getApplicationContext(), "생년월일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(birthMonth)) {
                    Toast.makeText(getApplicationContext(), "생년월일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(birthDay)) {
                    Toast.makeText(getApplicationContext(), "생년월일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                sign();
            }
        });
    }


    private void sign() {
        Log.i("HS SignActivity", "sign start");

        String id = editId.getText().toString();
        String pw = editPw.getText().toString();
        String tel = editTel.getText().toString();
        String email = editEmail.getText().toString();
        String birth = editBirthYear.getText().toString()+String.format("%02d", Integer.parseInt(editBirthMonth.getText().toString()))+String.format("%02d", Integer.parseInt(editBirthDay.getText().toString()));


        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "sign")
                .add("id", id)
                .add("pw", pw)
                .add("tel", tel)
                .add("email", email)
                .add("birth", birth)
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
                                Toast.makeText(getApplicationContext(), "회원가입되었습니다." + responseData, Toast.LENGTH_SHORT).show();

                                Intent intent = new Intent(SignActivity.this, LoginActivity.class);
                                startActivity(intent);

                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "다시 시도해주세요." + responseData, Toast.LENGTH_SHORT).show();
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