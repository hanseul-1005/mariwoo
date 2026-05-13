package com.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.helper.AlarmHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 약 수정 Activity
 *
 * 진입 시 전달받는 Intent 데이터:
 *   - medicine_no   : 약 번호 (서버 PK, 알람 requestCode 기준)
 *   - medicine_name : 약 이름 (표시용)
 *   - weekday       : 수정할 요일 (0:월 ~ 6:일)
 *
 * 주요 기능:
 *   1. loadDetail()   - 서버에서 해당 요일 스케줄 데이터 불러오기 → UI 초기화
 *   2. goModify()     - 수정 내용 서버 저장 → 해당 요일 알람 재등록
 *   3. deleteMedicine() - 약 soft delete → 모든 요일 알람 취소
 *
 * 스케줄 수정 로직:
 *   - scheduleNoMap에 기존 스케줄 PK 저장 (시간대명 → no)
 *   - 시간 지워진 항목은 delete_nos로 서버에 전달 → 스케줄 삭제
 *   - 기존 항목은 scheduleNo 기준 UPDATE
 *   - 새 항목은 INSERT
 */
public class MedicineModifyActivity extends AppCompatActivity {

    // 약 이름 표시 (수정 불가 - 약 이름은 변경 불가 정책)
    private EditText editName;

    // 아침/점심/저녁/취침 전 시간 표시 TextView
    private TextView tvMorningTime, tvLaunchTime, tvDinnerTime, tvNightTime;

    // 식전/식후 선택 RadioGroup
    private RadioGroup radioGroupMorning, radioGroupLaunch, radioGroupDinner, radioGroupNight;

    private SharedPreferences sharedPreferences;
    private String serverUrl    = "";
    private String medicineNo   = "";  // 약 번호 (알람 requestCode 기준)
    private String medicineName = "";  // 약 이름 (알림 표시용)
    private int    weekday      = 0;   // 수정 대상 요일 (MedicineListFragment에서 전달)

    // 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    // 기존 스케줄 PK 저장 맵
    // key: "아침"/"점심"/"저녁"/"취침 전"  value: medicine_schedule.no
    // → 수정 시 UPDATE 대상 판별, 삭제 시 delete_nos 구성에 사용
    private final Map<String, Long> scheduleNoMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_modify);

        // 루트: systemBars 패딩만 적용 → 버튼은 제자리 유지
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 스크롤 뷰: IME inset만큼 하단 패딩 → 키보드 등장 시 스크롤 영역만 축소
        NestedScrollView scrollView = findViewById(R.id.medicineModifyActivity_scrollView);
        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, insets) -> {
            Insets imeInsets  = insets.getInsets(WindowInsetsCompat.Type.ime());
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int imeExtra = Math.max(0, imeInsets.bottom - systemBars.bottom);
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeExtra);
            return insets;
        });

        // 포커스된 EditText가 키보드 바로 위에 오도록 자동 스크롤
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            View focused = scrollView.findFocus();
            if (focused != null) {
                Rect rect = new Rect();
                focused.getDrawingRect(rect);
                scrollView.requestChildRectangleOnScreen(focused, rect, false);
            }
        });

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);

        // MedicineListFragment에서 전달된 Intent 데이터 수신
        medicineNo   = getIntent().getStringExtra("medicine_no");
        medicineName = getIntent().getStringExtra("medicine_name");
        weekday      = getIntent().getIntExtra("weekday", 0);

        // 약 이름 표시 (수정 불가)
        editName = findViewById(R.id.medicineModifyActivity_editText_id);
        editName.setText(medicineName);
        editName.setEnabled(false);

        // 식전/식후 RadioGroup
        radioGroupMorning = findViewById(R.id.medicineModifyActivity_radioGroup_morning);
        radioGroupLaunch  = findViewById(R.id.medicineModifyActivity_radioGroup_launch);
        radioGroupDinner  = findViewById(R.id.medicineModifyActivity_radioGroup_dinner);
        radioGroupNight   = findViewById(R.id.medicineModifyActivity_radioGroup_night);

        // 시간 표시 TextView
        tvMorningTime = findViewById(R.id.medicineModifyActivity_text_morning_time);
        tvLaunchTime  = findViewById(R.id.medicineModifyActivity_text_launch_time);
        tvDinnerTime  = findViewById(R.id.medicineModifyActivity_text_dinner_time);
        tvNightTime   = findViewById(R.id.medicineModifyActivity_text_night_time);

        // 시간 영역 클릭 → TimePicker
        findViewById(R.id.medicineModifyActivity_layout_morning_time).setOnClickListener(v -> showTimePicker(tvMorningTime));
        findViewById(R.id.medicineModifyActivity_layout_launch_time).setOnClickListener(v -> showTimePicker(tvLaunchTime));
        findViewById(R.id.medicineModifyActivity_layout_dinner_time).setOnClickListener(v -> showTimePicker(tvDinnerTime));
        findViewById(R.id.medicineModifyActivity_layout_night_time).setOnClickListener(v -> showTimePicker(tvNightTime));

        // 수정 버튼
        AppCompatButton btnModify = findViewById(R.id.medicineModifyActivity_button_modify);
        btnModify.setText("수정");
        btnModify.setOnClickListener(v -> goModify());

        // 뒤로가기 버튼 → RESULT_OK 반환 (목록 새로고침 트리거)
        ImageView imgBack = findViewById(R.id.medicineModifyActivity_imageView_back);
        imgBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        // 전체 약 알람 삭제 버튼 → 확인 다이얼로그
        AppCompatButton btnDelete = findViewById(R.id.medicineModifyActivity_button_delete);
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        // 화면 진입 시 기존 데이터 로드
        loadDetail();
    }

    /**
     * 약 삭제 확인 다이얼로그
     * 확인 클릭 시 deleteMedicine() 호출
     */
    private void showDeleteConfirmDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("약 삭제")
                .setMessage("'" + medicineName + "' 약을 삭제하시겠습니까?")
                .setPositiveButton("삭제",  (dialog, which) -> deleteMedicine())
                .setNegativeButton("취소", null)
                .show();
    }

    /**
     * 약 삭제 (Soft Delete)
     * 서버에 del='Y' 처리 요청 후 모든 요일의 알람 취소
     * 완료 후 목록 화면으로 돌아감 (RESULT_OK)
     */
    private void deleteMedicine() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd",        "delete_medicine")
                .add("medicine_no", medicineNo)
                .add("weekday",    String.valueOf(weekday)) // 참고용 (서버에서 사용 가능)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(getApplicationContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;
                final String responseData = response.body().string();
                Log.i("HS", "deleteMedicine 응답 : " + responseData);

                // 빈 응답 방어 처리
                if (responseData == null || responseData.trim().isEmpty()) {
                    Log.e("HS", "deleteMedicine: 서버 응답이 비어있음");
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(responseData);
                        if ("true".equals(json.optString("result"))) {
                            // 모든 요일 × 모든 시간대 알람 취소 (0~6 × 0~3 = 최대 28개)
                            AlarmHelper.cancelAllAlarms(getApplicationContext(), medicineNo);
                            Toast.makeText(getApplicationContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        // 뒤로가기 시 RESULT_OK 반환 → 목록 새로고침
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    /**
     * 시간 선택 다이얼로그 (MaterialTimePicker, 24시간제 키보드 입력)
     * AddActivity와 동일한 방식
     * @param targetTextView 선택된 시간을 표시할 TextView
     */
    private void showTimePicker(TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        int hour   = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD) // 키보드 입력 모드
                .setTimeFormat(TimeFormat.CLOCK_24H)                  // 24시간제
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("시간 선택")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            // HH:mm 포맷으로 TextView에 시간 설정
            String time = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            targetTextView.setText(time);
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    /**
     * RadioGroup에서 선택된 식전/식후 값 반환
     * @return "식전", "식후", 또는 "" (선택 없음)
     */
    private String getIntakeType(RadioGroup radioGroup) {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) return ""; // 선택 없음
        RadioButton rb = findViewById(checkedId);
        return rb.getText().toString();
    }

    /**
     * 서버에서 해당 요일 스케줄 데이터 불러오기
     * 응답 데이터로 UI 초기화 및 scheduleNoMap 구성
     * scheduleNoMap은 이후 수정/삭제 시 스케줄 PK 조회에 사용
     */
    private void loadDetail() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd",        "get_medicine_detail")
                .add("medicine_no", medicineNo)
                .add("weekday",    String.valueOf(weekday)) // 해당 요일 스케줄만 조회
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;

                try {
                    String responseData = response.body().string();
                    Log.i("HS", "medicine_no : " + medicineNo);
                    Log.i("HS", "weekday : " + weekday);
                    Log.i("HS", "loadDetail 응답 : " + responseData);

                    // 빈 응답 방어 처리 (서버 예외 시 body가 빈 문자열로 올 수 있음)
                    if (responseData == null || responseData.trim().isEmpty()) {
                        Log.e("HS", "loadDetail: 서버 응답이 비어있음");
                        return;
                    }

                    JSONObject json = new JSONObject(responseData);

                    if ("true".equals(json.getString("result"))) {
                        JSONArray list = json.getJSONArray("list");

                        // 시간대별 시간/식전식후 임시 변수
                        String morningTime = "", launchTime = "", dinnerTime = "", nightTime = "";
                        String morningType = "", launchType = "", dinnerType = "", nightType = "";

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj          = list.getJSONObject(i);
                            long       no           = obj.getLong("no");            // 스케줄 PK
                            String     intakeTimeType = obj.getString("intake_time_type"); // 아침/점심 등
                            String     intakeTime   = obj.getString("intake_time"); // HH:mm
                            String     intakeType   = obj.getString("intake_type"); // 식전/식후

                            // 시간대별로 분류 + scheduleNoMap에 PK 저장
                            switch (intakeTimeType) {
                                case "아침":
                                    morningTime = intakeTime; morningType = intakeType;
                                    scheduleNoMap.put("아침", no);
                                    break;
                                case "점심":
                                    launchTime = intakeTime; launchType = intakeType;
                                    scheduleNoMap.put("점심", no);
                                    break;
                                case "저녁":
                                    dinnerTime = intakeTime; dinnerType = intakeType;
                                    scheduleNoMap.put("저녁", no);
                                    break;
                                case "취침 전":
                                    nightTime = intakeTime; nightType = intakeType;
                                    scheduleNoMap.put("취침 전", no);
                                    break;
                            }
                        }

                        // 람다에서 사용하기 위해 effectively final 변수로 복사
                        final String fMorningTime = morningTime, fLaunchTime = launchTime;
                        final String fDinnerTime  = dinnerTime,  fNightTime  = nightTime;
                        final String fMorningType = morningType, fLaunchType = launchType;
                        final String fDinnerType  = dinnerType,  fNightType  = nightType;

                        // UI 업데이트 (메인 스레드)
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            // 데이터 있는 시간대만 TextView 업데이트
                            if (!fMorningTime.isEmpty()) tvMorningTime.setText(fMorningTime);
                            if (!fLaunchTime.isEmpty())  tvLaunchTime.setText(fLaunchTime);
                            if (!fDinnerTime.isEmpty())  tvDinnerTime.setText(fDinnerTime);
                            if (!fNightTime.isEmpty())   tvNightTime.setText(fNightTime);

                            // 식전/식후 RadioButton 선택 복원
                            setIntakeType(radioGroupMorning, fMorningType);
                            setIntakeType(radioGroupLaunch,  fLaunchType);
                            setIntakeType(radioGroupDinner,  fDinnerType);
                            setIntakeType(radioGroupNight,   fNightType);
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * RadioGroup에서 주어진 intakeType과 일치하는 RadioButton 선택
     * @param intakeType "식전" 또는 "식후"
     */
    private void setIntakeType(RadioGroup radioGroup, String intakeType) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) radioGroup.getChildAt(i);
            if (rb.getText().toString().equals(intakeType)) {
                rb.setChecked(true);
                break;
            }
        }
    }

    /**
     * 약 스케줄 수정 요청
     *
     * 처리 과정:
     *   1. 각 시간대 UI에서 값 수집
     *   2. 기존에 있었는데 지워진 항목 → delete_nos에 추가
     *   3. 서버에 modify_medicine 요청
     *   4. 성공 시 해당 요일 알람 취소 후 새 알람 등록
     */
    private void goModify() {
        // 각 시간대 데이터 수집
        String intakeTimeType1 = "아침";
        String intakeType1     = getIntakeType(radioGroupMorning);
        // "시간 선택" 기본 텍스트면 빈 문자열 (미입력으로 처리)
        String intakeTime1     = tvMorningTime.getText().toString().equals("시간 선택") ? "" : tvMorningTime.getText().toString();

        String intakeTimeType2 = "점심";
        String intakeType2     = getIntakeType(radioGroupLaunch);
        String intakeTime2     = tvLaunchTime.getText().toString().equals("시간 선택") ? "" : tvLaunchTime.getText().toString();

        String intakeTimeType3 = "저녁";
        String intakeType3     = getIntakeType(radioGroupDinner);
        String intakeTime3     = tvDinnerTime.getText().toString().equals("시간 선택") ? "" : tvDinnerTime.getText().toString();

        String intakeTimeType4 = "취침 전";
        String intakeType4     = getIntakeType(radioGroupNight);
        String intakeTime4     = tvNightTime.getText().toString().equals("시간 선택") ? "" : tvNightTime.getText().toString();

        // 최소 하나 이상 시간 입력 필수
        if (intakeTime1.isEmpty() && intakeTime2.isEmpty() && intakeTime3.isEmpty() && intakeTime4.isEmpty()) {
            Toast.makeText(this, "복용 시간을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 기존에 있었는데 지워진 시간대 → 서버에서 해당 스케줄 삭제
        String[] timeTypes   = {"아침", "점심", "저녁", "취침 전"};
        String[] intakeTimes = {intakeTime1, intakeTime2, intakeTime3, intakeTime4};
        StringBuilder deleteNos = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            // 시간이 비어있고 기존에 스케줄이 있었던 항목 → 삭제 대상
            if (intakeTimes[i].isEmpty() && scheduleNoMap.containsKey(timeTypes[i])) {
                if (deleteNos.length() > 0) deleteNos.append(",");
                deleteNos.append(scheduleNoMap.get(timeTypes[i]));
            }
        }

        // 람다 내부에서 사용하기 위해 effectively final로 복사
        final String fIntakeTime1 = intakeTime1, fIntakeType1 = intakeType1;
        final String fIntakeTime2 = intakeTime2, fIntakeType2 = intakeType2;
        final String fIntakeTime3 = intakeTime3, fIntakeType3 = intakeType3;
        final String fIntakeTime4 = intakeTime4, fIntakeType4 = intakeType4;

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",             "modify_medicine")
                .add("medicine_no",     medicineNo)
                .add("weekday",         String.valueOf(weekday))
                .add("intake_time_type1", intakeTimeType1)
                .add("intake_time1",    intakeTime1)
                .add("intake_type1",    intakeType1)
                .add("intake_time_type2", intakeTimeType2)
                .add("intake_time2",    intakeTime2)
                .add("intake_type2",    intakeType2)
                .add("intake_time_type3", intakeTimeType3)
                .add("intake_time3",    intakeTime3)
                .add("intake_type3",    intakeType3)
                .add("intake_time_type4", intakeTimeType4)
                .add("intake_time4",    intakeTime4)
                .add("intake_type4",    intakeType4)
                // 기존 스케줄 PK (-1이면 신규 INSERT, 그 외 UPDATE)
                .add("no1", String.valueOf(scheduleNoMap.getOrDefault("아침",    -1L)))
                .add("no2", String.valueOf(scheduleNoMap.getOrDefault("점심",    -1L)))
                .add("no3", String.valueOf(scheduleNoMap.getOrDefault("저녁",    -1L)))
                .add("no4", String.valueOf(scheduleNoMap.getOrDefault("취침 전", -1L)))
                .add("delete_nos",      deleteNos.toString()) // 삭제할 스케줄 PK 목록 (쉼표 구분)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;

                try {
                    String responseData = response.body().string();
                    Log.i("HS", "goModify 응답 : " + responseData);

                    // 빈 응답 방어 처리
                    if (responseData == null || responseData.trim().isEmpty()) {
                        Log.e("HS", "goModify: 서버 응답이 비어있음");
                        runOnUiThread(() -> Toast.makeText(getApplicationContext(), "서버 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    JSONObject json = new JSONObject(responseData);

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        if ("true".equals(json.optString("result"))) {
                            Toast.makeText(getApplicationContext(), "수정되었습니다.", Toast.LENGTH_SHORT).show();

                            // 해당 요일 기존 알람(4개 시간대) 취소 후 새 알람 등록
                            cancelAlarms();
                            setAlarms(fIntakeTime1, fIntakeType1,
                                    fIntakeTime2,  fIntakeType2,
                                    fIntakeTime3,  fIntakeType3,
                                    fIntakeTime4,  fIntakeType4);

                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "수정 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 해당 요일의 4개 시간대 알람 모두 취소
     * 수정 전 기존 알람 제거용 (이후 setAlarms()로 새로 등록)
     */
    private void cancelAlarms() {
        for (int i = 0; i < 4; i++) { // 0:아침 ~ 3:취침 전
            AlarmHelper.cancelAlarm(getApplicationContext(), medicineNo, String.valueOf(weekday), i);
        }
    }

    /**
     * 입력된 시간이 있는 시간대만 알람 등록
     * 빈 시간대는 skip (AlarmHelper.setAlarm 호출 안 함)
     */
    private void setAlarms(String intakeTime1, String intakeType1,
                           String intakeTime2, String intakeType2,
                           String intakeTime3, String intakeType3,
                           String intakeTime4, String intakeType4) {
        if (!intakeTime1.isEmpty())
            AlarmHelper.setAlarm(getApplicationContext(), medicineName, medicineNo, String.valueOf(weekday), 0, intakeType1, intakeTime1);
        if (!intakeTime2.isEmpty())
            AlarmHelper.setAlarm(getApplicationContext(), medicineName, medicineNo, String.valueOf(weekday), 1, intakeType2, intakeTime2);
        if (!intakeTime3.isEmpty())
            AlarmHelper.setAlarm(getApplicationContext(), medicineName, medicineNo, String.valueOf(weekday), 2, intakeType3, intakeTime3);
        if (!intakeTime4.isEmpty())
            AlarmHelper.setAlarm(getApplicationContext(), medicineName, medicineNo, String.valueOf(weekday), 3, intakeType4, intakeTime4);
    }
}
