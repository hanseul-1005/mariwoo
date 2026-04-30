package com.windy.mariwoo.basic.user;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.LoginActivity;
import com.windy.mariwoo.basic.TosActivity;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
    private EditText editName;
    private EditText editTel;
    private TextInputEditText editEmail;
    private AutoCompleteTextView editAddr;
    private TextInputLayout layoutEmailDirect;
    private TextInputEditText editAddrDirect;
    private Button idCheck;


    private final String[] emailDomains = {
            "gmail.com",
            "naver.com",
            "hanmail.net",
            "kakao.com",
            "outlook.com",
            "hotmail.com",
            "직접입력"
    };

    private TextInputEditText editBirth;
    private Button btnSign;

    private String serverUrl = "";
    private boolean idCheckResult = false;
    private boolean pwCheckResult = false;

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
        editName = findViewById(R.id.signActivity_editText_name);
        editTel = findViewById(R.id.signActivity_editText_tel);
        editBirth = findViewById(R.id.signActivity_editText_birth);

        editBirth.setOnClickListener(v -> showBirthDatePicker());
        editBirth.setTextColor(getResources().getColor(android.R.color.black));

        idCheck = findViewById(R.id.signActivity_button_idCheck);
        idCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCheckId();
            }
        });

        String pw = editPw.getText().toString();
        String pwCheck = editPwCheck.getText().toString();

        editPw.addTextChangedListener(new TextWatcher() {
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

                            int textColor = ContextCompat.getColor(SignActivity.this, R.color.color_r);
                            tvPwCheck.setTextColor(textColor);

                            pwCheckResult = false;
                        } else {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                            pwCheckResult = true;
                        }
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
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

                            int textColor = ContextCompat.getColor(SignActivity.this, R.color.color_r);
                            tvPwCheck.setTextColor(textColor);

                            pwCheckResult = false;
                        } else {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                            pwCheckResult = true;
                        }
                    }
                });

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        editTel.addTextChangedListener(new TextWatcher() {
            boolean isFormatting;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                String onlyNumber = s.toString().replaceAll("[^0-9]", "");
                String result;

                if (onlyNumber.length() > 11) {
                    onlyNumber = onlyNumber.substring(0, 11);
                }

                if (onlyNumber.length() <= 3) {
                    result = onlyNumber;
                } else if (onlyNumber.length() <= 7) {
                    result = onlyNumber.substring(0, 3) + "-" + onlyNumber.substring(3);
                } else {
                    result = onlyNumber.substring(0, 3) + "-"
                            + onlyNumber.substring(3, 7) + "-"
                            + onlyNumber.substring(7);
                }

                editTel.setText(result);
                editTel.setSelection(result.length());

                isFormatting = false;
            }
        });

        editEmail = findViewById(R.id.signActivity_textField_email);
        editAddr = findViewById(R.id.signActivity_textView_emailAddr);
        layoutEmailDirect = findViewById(R.id.signActivity_textInputLayout_emailAddr);
        editAddrDirect = findViewById(R.id.signActivity_textInput_emailAddr);


        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                emailDomains
        );

        editAddr.setAdapter(adapter);
        editAddr.setText(emailDomains[0], false);

        editAddr.setOnItemClickListener((parent, view, position, id) -> {
            String selected = editAddr.getText().toString();

            if ("직접입력".equals(selected)) {
                layoutEmailDirect.setVisibility(View.VISIBLE);
                editAddrDirect.setText("");
                editAddrDirect.requestFocus();
            } else {
                layoutEmailDirect.setVisibility(View.GONE);
                editAddrDirect.setText("");
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
                check();
            }
        });
    }

    private void showBirthDatePicker() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        utc.set(1920, Calendar.JANUARY, 1);
        long startDate = utc.getTimeInMillis();

        long endDate = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(endDate)
                .setOpenAt(endDate)
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("생년월일 선택")
                .setSelection(endDate)
                .setCalendarConstraints(constraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            String birth = sdf.format(new Date(selection));
            editBirth.setText(birth);
        });

        picker.show(getSupportFragmentManager(), "birth_picker");
    }


    private void getCheckId() {
        Log.i("HS SignActivity", "id check start");

        String id = editId.getText().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "check_id")
                .add("id", id)
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
                                idCheckResult = true;
                                Toast.makeText(getApplicationContext(), "사용 가능한 아이디 입니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                idCheckResult = false;
                                Toast.makeText(getApplicationContext(), "중복된 아이디 입니다.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
    }
    private void sign() {
        Log.i("HS SignActivity", "sign start");

        String id = editId.getText().toString();
        String pw = editPw.getText().toString();
        String name = editName.getText().toString();
        String tel = editTel.getText().toString();
        String userEmail = "";
        String emailId = editEmail.getText().toString();
        String emailAddr = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();

        if("직접 입력".equals(emailAddr)) {
            userEmail = emailId+"@"+emailAddrDirect;
        } else {
            userEmail = emailId+"@"+emailAddr;
        }
        String birth = editBirth.getText().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "sign")
                .add("id", id)
                .add("pw", pw)
                .add("name", name)
                .add("tel", tel)
                .add("email", userEmail)
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

    private void check() {

        String id = editId.getText().toString();
        String pw = editPw.getText().toString();
        String name = editName.getText().toString();
        String tel = editTel.getText().toString();
        String email = editEmail.getText().toString();
        String emailAddr = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();
        String birth = editBirth.getText().toString();

        if("".equals(id)) {
            Toast.makeText(getApplicationContext(), "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(idCheckResult==false) {
            Toast.makeText(getApplicationContext(), "아이디 중복확인을 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if("".equals(pw)) {
            Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if("".equals(name)) {
            Toast.makeText(getApplicationContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
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

        if ("직접입력".equals(emailAddr)) {
            if("".equals(emailAddrDirect)) {
                Toast.makeText(getApplicationContext(), "이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if("".equals(birth)) {
            Toast.makeText(getApplicationContext(), "생년월일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        sign();
    }
}