package com.windy.mariwoo.basic.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.LoginActivity;
import com.windy.mariwoo.basic.MainActivity;

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
 * 가족(관계) 추가 신청 Activity
 *
 * 동작 흐름:
 *   1. 상대방의 아이디와 전화번호 입력
 *   2. 확인 버튼 → reqRelation() 호출 → 서버에 열람 관계 신청
 *   3. 서버에서 대상자가 수락하면 RelationListFragment에서 "수락" 처리 가능
 *
 * 서버 cmd: "req_relation"
 *   전송: user_no (내 번호), target_id (상대 아이디), target_tel (상대 전화번호)
 *   응답: {"result":"true"} 또는 {"result":"false"}
 */
public class RelationAddActivity extends AppCompatActivity {

    private EditText editId;  // 상대방 아이디 입력
    private EditText editTel; // 상대방 전화번호 입력
    private Button   btnOk;     // 신청 확인 버튼
    private Button   btnCancel; // 취소 버튼

    private SharedPreferences sharedPreferences;
    private String userNo    = ""; // 내 user_no (SharedPreferences에서 로드)
    private String serverUrl = "";

    // 싱글톤 OkHttpClient (매 요청마다 생성 방지)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_relation_add);

        // 시스템 바 패딩 처리
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 다크 모드 비활성화
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        serverUrl = getString(R.string.server_user);

        // 내 user_no 로드 (로그인 시 저장된 값)
        sharedPreferences = getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        editId  = findViewById(R.id.relationAddActivity_editText_id);
        editTel = findViewById(R.id.relationAddActivity_editText_tel);

        // 신청 버튼
        btnOk = findViewById(R.id.relationAddActivity_button_ok);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reqRelation();
            }
        });

        // 취소 버튼 → 화면 종료
        btnCancel = findViewById(R.id.relationAddActivity_button_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    /**
     * 가족 열람 관계 신청 서버 요청
     * - 상대방 아이디 + 전화번호로 대상자를 식별해 신청
     * - 성공 시 Activity 종료 (목록 화면으로 복귀)
     * - 실패 시 안내 토스트 (일치하는 사용자 없음)
     */
    private void reqRelation() {
        Log.i("HS RelationAddActivity", "reqRelation start");

        String targetId  = editId.getText().toString();
        String targetTel = editTel.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",        "req_relation")
                .add("user_no",    userNo)
                .add("target_id",  targetId)
                .add("target_tel", targetTel)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS RelationAddActivity", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
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
                                Toast.makeText(getApplicationContext(), "신청되었습니다.", Toast.LENGTH_SHORT).show();
                                finish(); // 신청 완료 → 목록 화면으로 복귀
                            } else {
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
