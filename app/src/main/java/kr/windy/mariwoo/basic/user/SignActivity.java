package kr.windy.mariwoo.basic.user;

import android.content.Intent;
import android.graphics.Rect;
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
import androidx.core.widget.NestedScrollView;
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
import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.LoginActivity;
import kr.windy.mariwoo.basic.TosActivity;

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

/**
 * 회원가입 Activity
 *
 * 주요 기능:
 *   - 아이디 중복 확인 (getCheckId)
 *   - 비밀번호 / 비밀번호 확인 실시간 일치 여부 체크 (TextWatcher)
 *   - 전화번호 자동 하이픈 포맷 (000-0000-0000 형식)
 *   - 이메일 도메인 드롭다운 선택 (직접입력 시 추가 입력 필드 표시)
 *   - 생년월일 MaterialDatePicker (오늘 이전 날짜만 선택 가능)
 *   - 필수 입력 항목 유효성 검사 후 회원가입 서버 전송 (sign)
 *
 * 서버 cmd:
 *   - "check_id" : 아이디 중복 확인
 *   - "sign"     : 회원가입 정보 등록
 */
public class SignActivity extends AppCompatActivity {

    private ImageView imgBack; // 뒤로 가기 버튼

    private CheckBox chkUse;       // 이용 약관 동의 체크박스
    private CheckBox chkPrivacy;   // 개인정보 처리방침 동의
    private CheckBox chkMarketing; // 마케팅 정보 수신 동의

    private TextView tvUse;       // 이용 약관 상세 보기
    private TextView tvPrivacy;   // 개인정보 처리방침 상세 보기
    private TextView tvMarketing; // 마케팅 정보 수신 상세 보기

    private EditText          editId;          // 아이디 입력
    private EditText          editPw;          // 비밀번호 입력
    private EditText          editPwCheck;     // 비밀번호 확인 입력
    private TextView          tvPwCheck;       // 비밀번호 일치 여부 안내 메시지
    private EditText          editName;        // 이름 입력
    private EditText          editTel;         // 전화번호 입력 (자동 포맷)
    private TextInputEditText editEmail;       // 이메일 아이디 부분 입력
    private AutoCompleteTextView editAddr;     // 이메일 도메인 드롭다운
    private TextInputLayout   layoutEmailDirect; // "직접입력" 선택 시 표시되는 레이아웃
    private TextInputEditText editAddrDirect; // 도메인 직접 입력 필드
    private Button            idCheck;        // 아이디 중복 확인 버튼

    // 이메일 도메인 선택 목록 ("직접입력" 선택 시 layoutEmailDirect 표시)
    private final String[] emailDomains = {
            "gmail.com",
            "naver.com",
            "hanmail.net",
            "kakao.com",
            "outlook.com",
            "hotmail.com",
            "직접입력"
    };

    private TextInputEditText editBirth; // 생년월일 선택 (MaterialDatePicker)
    private Button            btnSign;   // 회원가입 버튼

    private String  serverUrl      = "";
    private boolean idCheckResult  = false; // 아이디 중복 확인 완료 여부
    private boolean pwCheckResult  = false; // 비밀번호 일치 여부

    // 싱글톤 OkHttpClient (매 요청마다 생성 방지)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign);

        // 루트: systemBars 패딩만 적용 → 키보드가 올라와도 버튼은 제자리 유지
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 스크롤 뷰: IME inset만큼 하단 패딩 추가 → 키보드가 올라오면 스크롤 영역만 축소
        NestedScrollView scrollView = findViewById(R.id.signActivity_scrollView);
        int basePaddingPx = (int) (10 * getResources().getDisplayMetrics().density); // XML paddingBottom 10dp
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            // 키보드 높이에서 systemBars 높이를 뺀 만큼만 추가 패딩 적용
            int imeExtra = Math.max(0, imeInsets.bottom - systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), basePaddingPx + imeExtra);
            return insets;
        });

        // 키보드가 올라올 때 포커스된 EditText가 키보드 바로 위에 오도록 자동 스크롤
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

        // View 초기화
        editId      = findViewById(R.id.signActivity_editText_id);
        editPw      = findViewById(R.id.signActivity_editText_pw);
        editPwCheck = findViewById(R.id.signActivity_editText_chk_pw);
        tvPwCheck   = findViewById(R.id.signActivity_textView_check);
        editName    = findViewById(R.id.signActivity_editText_name);
        editTel     = findViewById(R.id.signActivity_editText_tel);
        editBirth   = findViewById(R.id.signActivity_editText_birth);

        // 생년월일 클릭 → MaterialDatePicker 표시
        editBirth.setOnClickListener(v -> showBirthDatePicker());
        editBirth.setTextColor(getResources().getColor(android.R.color.black));

        // 아이디 입력 변경 시 중복 확인 결과 초기화 (재확인 강제)
        editId.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                idCheckResult = false; // 아이디 바뀌면 중복 확인 무효화
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 아이디 중복 확인
        idCheck = findViewById(R.id.signActivity_button_idCheck);
        idCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCheckId();
            }
        });

        // 비밀번호 실시간 일치 확인 (editPw 변경 시)
        editPw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pw      = editPw.getText().toString();
                String pwCheck = editPwCheck.getText().toString();
                if (!pw.equals(pwCheck)) {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(SignActivity.this, R.color.color_r));
                    pwCheckResult = false;
                } else {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(SignActivity.this, R.color.theme_color));
                    pwCheckResult = true;
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // 비밀번호 확인 실시간 일치 확인 (editPwCheck 변경 시)
        editPwCheck.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pw      = editPw.getText().toString();
                String pwCheck = editPwCheck.getText().toString();
                if (!pw.equals(pwCheck)) {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(SignActivity.this, R.color.color_r));
                    pwCheckResult = false;
                } else {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(SignActivity.this, R.color.theme_color));
                    pwCheckResult = true;
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        // 전화번호 자동 하이픈 포맷: 000-0000-0000
        editTel.addTextChangedListener(new TextWatcher() {
            boolean isFormatting; // 재귀 호출 방지 플래그

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;

                // 숫자만 추출, 최대 11자리
                String onlyNumber = s.toString().replaceAll("[^0-9]", "");
                if (onlyNumber.length() > 11) {
                    onlyNumber = onlyNumber.substring(0, 11);
                }

                // 하이픈 삽입
                String result;
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
                editTel.setSelection(result.length()); // 커서를 끝으로 이동

                isFormatting = false;
            }
        });

        // 이메일 도메인 드롭다운 설정
        editEmail        = findViewById(R.id.signActivity_textField_email);
        editAddr         = findViewById(R.id.signActivity_textView_emailAddr);
        layoutEmailDirect = findViewById(R.id.signActivity_textInputLayout_emailAddr);
        editAddrDirect   = findViewById(R.id.signActivity_textInput_emailAddr);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                emailDomains
        );
        editAddr.setAdapter(adapter);
        editAddr.setText(emailDomains[0], false); // 기본값: gmail.com

        // "직접입력" 선택 시 추가 입력 필드 표시
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

        // 뒤로 가기
        imgBack = findViewById(R.id.signActivity_imageView_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 약관 상세 보기 → TosActivity로 이동 (type으로 종류 전달)
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

        // 회원가입 버튼 → 유효성 검사 후 sign() 호출
        btnSign = findViewById(R.id.signActivity_button_sign);
        btnSign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                check();
            }
        });

        // 이메일 입력 칸에서 Done 버튼 → 회원가입 버튼 액션 실행
        editEmail.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnSign.performClick();
                return true;
            }
            return false;
        });

        // 도메인 직접입력 칸에서 Done 버튼 → 회원가입 버튼 액션 실행
        editAddrDirect.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                btnSign.performClick();
                return true;
            }
            return false;
        });
    }

    /**
     * 생년월일 선택 다이얼로그 표시 (MaterialDatePicker)
     * - 1920년 1월 1일 ~ 오늘까지만 선택 가능
     * - 선택 완료 시 editBirth에 yyyy-MM-dd 형식으로 설정
     */
    private void showBirthDatePicker() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(1920, Calendar.JANUARY, 1);
        long startDate = utc.getTimeInMillis();
        long endDate   = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(endDate)
                .setOpenAt(endDate)
                .setValidator(DateValidatorPointBackward.now()) // 오늘 이전만 선택 가능
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("생년월일 선택")
                .setSelection(endDate)
                .setCalendarConstraints(constraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            String           birth = sdf.format(new Date(selection));
            editBirth.setText(birth);
        });

        picker.show(getSupportFragmentManager(), "birth_picker");
    }

    /**
     * 아이디 중복 확인 서버 요청
     * - cmd: "check_id", id 전송
     * - 사용 가능 → idCheckResult = true
     * - 중복 → idCheckResult = false
     *
     * idCheckResult가 false인 상태에서는 check()에서 회원가입을 막음
     */
    private void getCheckId() {
        Log.i("HS SignActivity", "id check start");

        String id = editId.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "check_id")
                .add("id",  id)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS SignActivity", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.body() == null) return;
                final String responseData = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("HS", "응답 성공");

                            JSONObject json   = new JSONObject(responseData);
                            String     result = json.getString("result");

                            if ("true".equals(result)) {
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

    /**
     * 회원가입 서버 요청
     * - cmd: "sign" + 입력 정보 전송
     * - 성공 시 LoginActivity로 이동
     * - 이메일: "직접 입력" 도메인이면 직접 입력한 값 사용, 아니면 드롭다운 값 사용
     */
    private void sign() {
        Log.i("HS SignActivity", "sign start");

        String id              = editId.getText().toString();
        String pw              = editPw.getText().toString();
        String name            = editName.getText().toString();
        String tel             = editTel.getText().toString();
        String emailId         = editEmail.getText().toString();
        String emailAddr       = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();

        // 이메일 조합 (직접입력 여부에 따라)
        String userEmail;
        if ("직접입력".equals(emailAddr)) {
            userEmail = emailId + "@" + emailAddrDirect;
        } else {
            userEmail = emailId + "@" + emailAddr;
        }

        String birth = editBirth.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",   "sign")
                .add("id",    id)
                .add("pw",    pw)
                .add("name",  name)
                .add("tel",   tel)
                .add("email", userEmail)
                .add("birth", birth)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS SignActivity", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.body() == null) return;
                final String responseData = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("HS", "응답 성공");

                            JSONObject json   = new JSONObject(responseData);
                            String     result = json.getString("result");

                            if ("true".equals(result)) {
                                Toast.makeText(getApplicationContext(), "회원가입되었습니다.", Toast.LENGTH_SHORT).show();

                                // 로그인 화면으로 이동 후 현재 Activity 종료
                                Intent intent = new Intent(SignActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();

                            } else {
                                Toast.makeText(getApplicationContext(), "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    /**
     * 회원가입 전 입력값 유효성 검사
     * - 아이디, 아이디 중복 확인, 비밀번호, 이름, 연락처, 이메일, 생년월일 순서로 검증
     * - 모두 통과하면 sign() 호출
     */
    private void check() {
        String id              = editId.getText().toString();
        String pw              = editPw.getText().toString();
        String name            = editName.getText().toString();
        String tel             = editTel.getText().toString();
        String email           = editEmail.getText().toString();
        String emailAddr       = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();
        String birth           = editBirth.getText().toString();

        if ("".equals(id)) {
            Toast.makeText(getApplicationContext(), "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!idCheckResult) {
            Toast.makeText(getApplicationContext(), "아이디 중복확인을 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(pw)) {
            Toast.makeText(getApplicationContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwCheckResult) {
            Toast.makeText(getApplicationContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(name)) {
            Toast.makeText(getApplicationContext(), "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(tel)) {
            Toast.makeText(getApplicationContext(), "연락처를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("".equals(email)) {
            Toast.makeText(getApplicationContext(), "이메일를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ("직접입력".equals(emailAddr)) {
            if ("".equals(emailAddrDirect)) {
                Toast.makeText(getApplicationContext(), "이메일 주소를 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if ("".equals(birth)) {
            Toast.makeText(getApplicationContext(), "생년월일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        sign();
    }
}
