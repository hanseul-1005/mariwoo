package kr.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.widget.NestedScrollView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.helper.AlarmHelper;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MedicineAddActivity extends AppCompatActivity {

    private ImageView imgBack;
    private EditText editName;
    private FrameLayout layoutLoading;
    private AppCompatButton btnRegister;

    private List<AppCompatButton> dayButtons = new ArrayList<>();

    // 시간 TextView
    private TextView tvMorningTime, tvLaunchTime, tvDinnerTime, tvNightTime;

    // 식전/식후 RadioGroup
    private RadioGroup radioGroupMorning, radioGroupLaunch, radioGroupDinner, radioGroupNight;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";

    // ✅ 싱글톤 OkHttpClient (매 요청마다 생성하지 않음)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_add);
        // 루트: systemBars 패딩만 적용 → 버튼은 제자리 유지
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 스크롤 뷰: IME inset만큼 하단 패딩 → 키보드 등장 시 스크롤 영역만 축소
        NestedScrollView scrollView = findViewById(R.id.medicineAddActivity_scrollView);
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
        userNo = sharedPreferences.getString("user_no", "-1");

        editName = findViewById(R.id.medicineAddActivity_editText_id);
        layoutLoading = findViewById(R.id.medicineAddActivity_layout_loading);

        // onCreate 안에 추가
        checkExactAlarmPermission();

        // 요일 버튼
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_mon));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_tue));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_wed));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_thu));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_fri));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_sat));
        dayButtons.add(findViewById(R.id.medicineAddActivity_button_sun));

        for (AppCompatButton btn : dayButtons) {
            btn.setOnClickListener(v -> v.setSelected(!v.isSelected()));
        }

        // 식전/식후 RadioGroup
        radioGroupMorning = findViewById(R.id.medicineAddActivity_radioGroup_morning);
        radioGroupLaunch  = findViewById(R.id.medicineAddActivity_radioGroup_launch);
        radioGroupDinner  = findViewById(R.id.medicineAddActivity_radioGroup_dinner);
        radioGroupNight   = findViewById(R.id.medicineAddActivity_radioGroup_night);

        // 시간 TextView
        tvMorningTime = findViewById(R.id.medicineAddActivity_text_morning_time);
        tvLaunchTime  = findViewById(R.id.medicineAddActivity_text_launch_time);
        tvDinnerTime  = findViewById(R.id.medicineAddActivity_text_dinner_time);
        tvNightTime   = findViewById(R.id.medicineAddActivity_text_night_time);

        // 시간 선택 클릭
        findViewById(R.id.medicineAddActivity_layout_morning_time).setOnClickListener(v -> showTimePicker(tvMorningTime));
        findViewById(R.id.medicineAddActivity_layout_launch_time).setOnClickListener(v -> showTimePicker(tvLaunchTime));
        findViewById(R.id.medicineAddActivity_layout_dinner_time).setOnClickListener(v -> showTimePicker(tvDinnerTime));
        findViewById(R.id.medicineAddActivity_layout_night_time).setOnClickListener(v -> showTimePicker(tvNightTime));

        // 등록 버튼
        btnRegister = findViewById(R.id.medicineAddActivity_button_login);
        btnRegister.setOnClickListener(v -> goAdd());

        // 뒤로가기
        imgBack = findViewById(R.id.medicineAddActivity_imageView_back);
        imgBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
        // fullScreenIntent 권한은 checkExactAlarmPermission()에서 통합 처리
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void showTimePicker(TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
/*
        new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    targetTextView.setText(time);
                },
                hour, minute, true).show();*/

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("시간 선택")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            String time = String.format("%02d:%02d", picker.getHour(), picker.getMinute());
            targetTextView.setText(time);
        });

        picker.show(getSupportFragmentManager(), "time_picker");
    }

    // 식전/식후 RadioGroup에서 값 가져오기
    // 선택 안했으면 "" 반환
    private String getIntakeType(RadioGroup radioGroup) {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) return "";
        RadioButton rb = findViewById(checkedId);
        return rb.getText().toString(); // "식전" or "식후"
    }

    private void goAdd() {
        Log.i("HS MedicineAddActivity", "add start");

        // 약 이름
        String name = editName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "약 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 선택된 요일 (0:월 ~ 6:일), 쉼표로 구분해서 전송
        // 예: "0,2,4" → 월,수,금
        StringBuilder weekdayBuilder = new StringBuilder();
        boolean anyDaySelected = false;
        for (int i = 0; i < dayButtons.size(); i++) {
            if (dayButtons.get(i).isSelected()) {
                if (anyDaySelected) weekdayBuilder.append(",");
                weekdayBuilder.append(i);
                anyDaySelected = true;
            }
        }
        if (!anyDaySelected) {
            Toast.makeText(this, "복용 요일을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        String weekday = weekdayBuilder.toString();

        // 아침 (intake_time_type: 0, 식전/식후, 시간)
        String intakeTimeType1 = "아침"; // 0:아침
        String intakeType1     = getIntakeType(radioGroupMorning); // 식전 or 식후
        String intakeTime1     = tvMorningTime.getText().toString().equals("시간 선택") ? "" : tvMorningTime.getText().toString();

        // 점심 (intake_time_type: 1)
        String intakeTimeType2 = "점심"; // 1:점심
        String intakeType2     = getIntakeType(radioGroupLaunch);
        String intakeTime2     = tvLaunchTime.getText().toString().equals("시간 선택") ? "" : tvLaunchTime.getText().toString();

        // 저녁 (intake_time_type: 2)
        String intakeTimeType3 = "저녁"; // 2:저녁
        String intakeType3     = getIntakeType(radioGroupDinner);
        String intakeTime3     = tvDinnerTime.getText().toString().equals("시간 선택") ? "" : tvDinnerTime.getText().toString();

        // 취침 전 (intake_time_type: 3)
        String intakeTimeType4 = "취침 전"; // 3:취침 전
        String intakeType4     = getIntakeType(radioGroupNight);
        String intakeTime4     = tvNightTime.getText().toString().equals("시간 선택") ? "" : tvNightTime.getText().toString();

        // 아침/점심/저녁/취침 중 하나 이상 시간 입력 확인
        if (intakeTime1.isEmpty() && intakeTime2.isEmpty() && intakeTime3.isEmpty() && intakeTime4.isEmpty()) {
            Toast.makeText(this, "복용 시간을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "add_medicine")
                .add("user_no", userNo)
                .add("name", name)
                .add("weekday", weekday)
                .add("intake_time_type1", intakeTimeType1)
                .add("intake_time1", intakeTime1)
                .add("intake_type1", intakeType1)
                .add("intake_time_type2", intakeTimeType2)
                .add("intake_time2", intakeTime2)
                .add("intake_type2", intakeType2)
                .add("intake_time_type3", intakeTimeType3)
                .add("intake_time3", intakeTime3)
                .add("intake_type3", intakeType3)
                .add("intake_time_type4", intakeTimeType4)
                .add("intake_time4", intakeTime4)
                .add("intake_type4", intakeType4)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS MedicineAddActivity", "request : " + request.toString());

        layoutLoading.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // ✅ IO 작업(body 읽기)은 백그라운드 스레드에서 처리
                if (response.body() == null) {
                    runOnUiThread(() -> {
                        layoutLoading.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);
                    });
                    return;
                }
                final String responseData = response.body().string();
                Log.i("HS", "응답 성공 : " + responseData);

                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    layoutLoading.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    try {
                        JSONObject json = new JSONObject(responseData);
                        String result = json.getString("result");

                        if ("true".equals(result)) {
                            Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();

                            String medicineNo = json.optString("medicine_no", "");

                            // 알람 설정 전 권한 체크
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

                                if (!alarmManager.canScheduleExactAlarms()) {
                                    Toast.makeText(getApplicationContext(),
                                            "알람 권한이 없어 알림이 설정되지 않았습니다.\n설정에서 권한을 허용해주세요.",
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    setAlarms(name, medicineNo, weekday, intakeTime1, intakeType1,
                                            intakeTime2, intakeType2,
                                            intakeTime3, intakeType3,
                                            intakeTime4, intakeType4);
                                }
                            } else {
                                setAlarms(name, medicineNo, weekday, intakeTime1, intakeType1,
                                        intakeTime2, intakeType2,
                                        intakeTime3, intakeType3,
                                        intakeTime4, intakeType4);
                            }

                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "등록에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void setAlarms(String name, String medicineNo, String weekday,
                           String intakeTime1, String intakeType1,
                           String intakeTime2, String intakeType2,
                           String intakeTime3, String intakeType3,
                           String intakeTime4, String intakeType4) {

        if (!intakeTime1.isEmpty()) {
            AlarmHelper.setAlarm(getApplicationContext(), name, medicineNo, weekday, 0, intakeType1, intakeTime1);
        }
        if (!intakeTime2.isEmpty()) {
            AlarmHelper.setAlarm(getApplicationContext(), name, medicineNo, weekday, 1, intakeType2, intakeTime2);
        }
        if (!intakeTime3.isEmpty()) {
            AlarmHelper.setAlarm(getApplicationContext(), name, medicineNo, weekday, 2, intakeType3, intakeTime3);
        }
        if (!intakeTime4.isEmpty()) {
            AlarmHelper.setAlarm(getApplicationContext(), name, medicineNo, weekday, 3, intakeType4, intakeTime4);
        }
    }

    // 권한 체크 메서드
    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12 이상
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (!alarmManager.canScheduleExactAlarms()) {
                // 권한 없으면 설정 화면으로 이동
                new AlertDialog.Builder(this)
                        .setTitle("알람 권한 필요")
                        .setMessage("정확한 시간에 약 복용 알림을 받으려면 알람 권한이 필요합니다.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        }
        // ✅ 배터리 최적화 예외 안 되어있을 때만 요청
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent batteryIntent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            batteryIntent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(batteryIntent);
        }
        // ✅ Android 14+ fullScreenIntent 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (!nm.canUseFullScreenIntent()) {
                new AlertDialog.Builder(this)
                        .setTitle("전체화면 알림 권한 필요")
                        .setMessage("약 복용 알림을 전체화면으로 받으려면 권한이 필요합니다.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        }
        // ✅ 다른 앱 위에 표시 권한
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("다른 앱 위에 표시 권한 필요")
                    .setMessage("약 복용 알림 화면을 띄우려면 권한이 필요합니다.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            boolean alreadyNotified = sharedPreferences.getBoolean("alarm_permission_notified", false);

            if (alarmManager.canScheduleExactAlarms() && !alreadyNotified) {
                // 권한 허용됨 + 아직 안내 안했을 때만 표시
                Toast.makeText(this, "알람 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();

                // 다음번엔 표시 안 하도록 저장
                sharedPreferences.edit()
                        .putBoolean("alarm_permission_notified", true)
                        .apply();

            } else if (!alarmManager.canScheduleExactAlarms()) {
                // 권한 거부된 상태면 다시 알려줄 수 있도록 초기화
                sharedPreferences.edit()
                        .putBoolean("alarm_permission_notified", false)
                        .apply();
            }
        }
    }
}