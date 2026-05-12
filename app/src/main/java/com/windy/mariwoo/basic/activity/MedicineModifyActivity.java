package com.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
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

public class MedicineModifyActivity extends AppCompatActivity {

    private EditText editName;
    private TextView tvMorningTime, tvLaunchTime, tvDinnerTime, tvNightTime;
    private RadioGroup radioGroupMorning, radioGroupLaunch, radioGroupDinner, radioGroupNight;

    private SharedPreferences sharedPreferences;
    private String serverUrl = "";
    private String medicineNo = "";
    private String medicineName = "";
    private int weekday = 0; // ✅ Intent로 받은 요일

    // ✅ 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    // ✅ 기존 스케줄 no 저장 (UPDATE/DELETE/INSERT 구분용)
    private final Map<String, Long> scheduleNoMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medicine_modify);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);

        // ✅ Intent에서 medicine_no, medicine_name, weekday 수신
        medicineNo   = getIntent().getStringExtra("medicine_no");
        medicineName = getIntent().getStringExtra("medicine_name");
        weekday      = getIntent().getIntExtra("weekday", 0);

        editName = findViewById(R.id.medicineModifyActivity_editText_id);
        editName.setText(medicineName);
        editName.setEnabled(false);

        radioGroupMorning = findViewById(R.id.medicineModifyActivity_radioGroup_morning);
        radioGroupLaunch  = findViewById(R.id.medicineModifyActivity_radioGroup_launch);
        radioGroupDinner  = findViewById(R.id.medicineModifyActivity_radioGroup_dinner);
        radioGroupNight   = findViewById(R.id.medicineModifyActivity_radioGroup_night);

        tvMorningTime = findViewById(R.id.medicineModifyActivity_text_morning_time);
        tvLaunchTime  = findViewById(R.id.medicineModifyActivity_text_launch_time);
        tvDinnerTime  = findViewById(R.id.medicineModifyActivity_text_dinner_time);
        tvNightTime   = findViewById(R.id.medicineModifyActivity_text_night_time);

        findViewById(R.id.medicineModifyActivity_layout_morning_time).setOnClickListener(v -> showTimePicker(tvMorningTime));
        findViewById(R.id.medicineModifyActivity_layout_launch_time).setOnClickListener(v -> showTimePicker(tvLaunchTime));
        findViewById(R.id.medicineModifyActivity_layout_dinner_time).setOnClickListener(v -> showTimePicker(tvDinnerTime));
        findViewById(R.id.medicineModifyActivity_layout_night_time).setOnClickListener(v -> showTimePicker(tvNightTime));

        AppCompatButton btnModify = findViewById(R.id.medicineModifyActivity_button_modify);
        btnModify.setText("수정");
        btnModify.setOnClickListener(v -> goModify());

        ImageView imgBack = findViewById(R.id.medicineModifyActivity_imageView_back);
        imgBack.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });

        AppCompatButton btnDelete = findViewById(R.id.medicineModifyActivity_button_delete);
        btnDelete.setOnClickListener(v -> showDeleteConfirmDialog());

        loadDetail();
    }

    private void showDeleteConfirmDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("약 삭제")
                .setMessage("'" + medicineName + "' 약을 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> deleteMedicine())
                .setNegativeButton("취소", null)
                .show();
    }

    private void deleteMedicine() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "delete_medicine")
                .add("medicine_no", medicineNo)
                .add("weekday", String.valueOf(weekday))
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
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(responseData);
                        if ("true".equals(json.optString("result"))) {
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
        setResult(RESULT_OK);
        super.onBackPressed();
    }

    private void showTimePicker(TextView targetTextView) {
        Calendar calendar = Calendar.getInstance();
        int hour   = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        new TimePickerDialog(this,
                (view, selectedHour, selectedMinute) -> {
                    String time = String.format("%02d:%02d", selectedHour, selectedMinute);
                    targetTextView.setText(time);
                },
                hour, minute, true).show();
    }

    private String getIntakeType(RadioGroup radioGroup) {
        int checkedId = radioGroup.getCheckedRadioButtonId();
        if (checkedId == -1) return "";
        RadioButton rb = findViewById(checkedId);
        return rb.getText().toString();
    }

    // ✅ 해당 요일 데이터만 불러오기
    private void loadDetail() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "get_medicine_detail")
                .add("medicine_no", medicineNo)
                .add("weekday", String.valueOf(weekday)) // ✅ 요일 함께 전송
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

                    JSONObject json = new JSONObject(responseData);

                    if ("true".equals(json.getString("result"))) {
                        JSONArray list = json.getJSONArray("list");

                        String morningTime = "", launchTime = "", dinnerTime = "", nightTime = "";
                        String morningType = "", launchType = "", dinnerType = "", nightType = "";

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject obj = list.getJSONObject(i);
                            long   no            = obj.getLong("no");
                            String intakeTimeType = obj.getString("intake_time_type");
                            String intakeTime     = obj.getString("intake_time");
                            String intakeType     = obj.getString("intake_type");

                            // ✅ no를 scheduleNoMap에 저장
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

                        final String fMorningTime = morningTime, fLaunchTime = launchTime;
                        final String fDinnerTime  = dinnerTime,  fNightTime  = nightTime;
                        final String fMorningType = morningType, fLaunchType = launchType;
                        final String fDinnerType  = dinnerType,  fNightType  = nightType;

                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;

                            if (!fMorningTime.isEmpty()) tvMorningTime.setText(fMorningTime);
                            if (!fLaunchTime.isEmpty())  tvLaunchTime.setText(fLaunchTime);
                            if (!fDinnerTime.isEmpty())  tvDinnerTime.setText(fDinnerTime);
                            if (!fNightTime.isEmpty())   tvNightTime.setText(fNightTime);

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

    private void setIntakeType(RadioGroup radioGroup, String intakeType) {
        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            RadioButton rb = (RadioButton) radioGroup.getChildAt(i);
            if (rb.getText().toString().equals(intakeType)) {
                rb.setChecked(true);
                break;
            }
        }
    }

    private void goModify() {
        String intakeTimeType1 = "아침";
        String intakeType1     = getIntakeType(radioGroupMorning);
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

        if (intakeTime1.isEmpty() && intakeTime2.isEmpty() && intakeTime3.isEmpty() && intakeTime4.isEmpty()) {
            Toast.makeText(this, "복용 시간을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] timeTypes   = {"아침", "점심", "저녁", "취침 전"};
        String[] intakeTimes = {intakeTime1, intakeTime2, intakeTime3, intakeTime4};
        StringBuilder deleteNos = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            if (intakeTimes[i].isEmpty() && scheduleNoMap.containsKey(timeTypes[i])) {
                if (deleteNos.length() > 0) deleteNos.append(",");
                deleteNos.append(scheduleNoMap.get(timeTypes[i]));
            }
        }

        // ✅ 람다에서 쓸 effectively final 변수
        final String fIntakeTime1 = intakeTime1, fIntakeType1 = intakeType1;
        final String fIntakeTime2 = intakeTime2, fIntakeType2 = intakeType2;
        final String fIntakeTime3 = intakeTime3, fIntakeType3 = intakeType3;
        final String fIntakeTime4 = intakeTime4, fIntakeType4 = intakeType4;

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "modify_medicine")
                .add("medicine_no", medicineNo)
                .add("weekday", String.valueOf(weekday))
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
                .add("no1", String.valueOf(scheduleNoMap.getOrDefault("아침",    -1L)))
                .add("no2", String.valueOf(scheduleNoMap.getOrDefault("점심",    -1L)))
                .add("no3", String.valueOf(scheduleNoMap.getOrDefault("저녁",    -1L)))
                .add("no4", String.valueOf(scheduleNoMap.getOrDefault("취침 전", -1L)))
                .add("delete_nos", deleteNos.toString())
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
                    JSONObject json = new JSONObject(responseData);

                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;

                        if ("true".equals(json.optString("result"))) {
                            Toast.makeText(getApplicationContext(), "수정되었습니다.", Toast.LENGTH_SHORT).show();

                            // ✅ 해당 요일 기존 알람 취소 후 새로 등록
                            cancelAlarms();
                            setAlarms(fIntakeTime1, fIntakeType1,
                                    fIntakeTime2, fIntakeType2,
                                    fIntakeTime3, fIntakeType3,
                                    fIntakeTime4, fIntakeType4);

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

    // 해당 요일의 4개 시간대 알람만 취소
    private void cancelAlarms() {
        for (int i = 0; i < 4; i++) {
            AlarmHelper.cancelAlarm(getApplicationContext(), medicineNo, String.valueOf(weekday), i);
        }
    }

    // 비어있지 않은 시간만 알람 등록
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