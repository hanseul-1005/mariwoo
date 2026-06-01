package kr.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;

import kr.windy.mariwoo.R;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 병원 스케줄 등록 / 수정 Activity
 * - Intent extra "mode": "add" or "modify"
 * - 수정 시 "no", "name", "time", "memo" 전달
 */
public class HospitalAddActivity extends AppCompatActivity {

    private EditText        editName, editDate, editTime, editMemo;
    private AppCompatButton btnSave;

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";
    private String mode      = "add";
    private long   scheduleNo = -1;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_hospital_add);

        serverUrl = getString(R.string.server_hospital);
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        editName = findViewById(R.id.hospitalAddActivity_editText_name);
        editDate = findViewById(R.id.hospitalAddActivity_editText_date);
        editTime = findViewById(R.id.hospitalAddActivity_editText_time);
        editMemo = findViewById(R.id.hospitalAddActivity_editText_memo);
        btnSave  = findViewById(R.id.hospitalAddActivity_button_save);

        // Intent에서 모드 및 기존 데이터 수신
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "add";

        if ("modify".equals(mode)) {
            scheduleNo = getIntent().getLongExtra("no", -1);
            String time = getIntent().getStringExtra("time"); // "yyyy-MM-dd HH:mm"

            editName.setText(getIntent().getStringExtra("name"));
            editMemo.setText(getIntent().getStringExtra("memo"));
            btnSave.setText("병원 스케줄 수정");

            // time → date / time 분리
            if (time != null && time.contains(" ")) {
                String[] parts = time.split(" ");
                editDate.setText(parts[0]);
                editTime.setText(parts[1]);
            } else {
                editDate.setText(time);
            }
        } else {
            // 등록 모드: 오늘 날짜, 현재 시간 기본값
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            editDate.setText(sdf.format(Calendar.getInstance().getTime()));
            Calendar cal = Calendar.getInstance();
            editTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        }

        // 날짜 클릭 → DatePickerDialog
        editDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(editDate.getText().toString());
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(editDate.getText().toString()));
            } catch (Exception ignored) {}

            new DatePickerDialog(this, (view, year, month, day) -> {
                editDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 시간 클릭 → TimePickerDialog
        editTime.setOnClickListener(v -> {
            int hour = 9, minute = 0;
            try {
                String[] t = editTime.getText().toString().split(":");
                hour   = Integer.parseInt(t[0]);
                minute = Integer.parseInt(t[1]);
            } catch (Exception ignored) {}

            new TimePickerDialog(this, (view, h, m) -> {
                editTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
            }, hour, minute, true).show();
        });

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = editName.getText().toString().trim();
        String date = editDate.getText().toString();
        String time = editTime.getText().toString();
        String memo = editMemo.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "병원명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullTime = date + " " + time; // "yyyy-MM-dd HH:mm"

        FormBody.Builder builder = new FormBody.Builder()
                .add("name", name)
                .add("time", fullTime)
                .add("notification_time", fullTime)
                .add("memo", memo);

        if ("modify".equals(mode)) {
            builder.add("cmd", "modify");
            builder.add("no",  String.valueOf(scheduleNo));
        } else {
            builder.add("cmd",     "add");
            builder.add("user_no", userNo);
        }

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(builder.build())
                .build();

        Log.i("HS HospitalAdd", "mode=" + mode + " request=" + request);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { e.printStackTrace(); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;
                String responseData = response.body().string();

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if ("true".equals(json.getString("result"))) {
                            String msg = "modify".equals(mode) ? "수정되었습니다." : "등록되었습니다.";
                            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}
