package com.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

public class MedicineModifyActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";

    String no1 = "-1";
    String no2 = "-1";
    String no3 = "-1";
    String no4 = "-1";

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
        userNo = sharedPreferences.getString("user_no", "-1");

    }


    private void goModify() {
        Log.i("HS MedicineModifyActivity", "modify start");

        String name = "";
        String weekday = "";
        String intakeTimeType1 = "";
        String intakeTime1 = "";
        String intakeType1 = "";
        String intakeTimeType2 = "";
        String intakeTime2 = "";
        String intakeType2 = "";
        String intakeTimeType3 = "";
        String intakeTime3 = "";
        String intakeType3 = "";
        String intakeTimeType4 = "";
        String intakeTime4 = "";
        String intakeType4 = "";


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

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS MedicineModifyActivity", "request : "+request.toString());
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
                                Toast.makeText(getApplicationContext(), "등록되었습니다.", Toast.LENGTH_SHORT).show();
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