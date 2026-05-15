package kr.windy.mariwoo.basic.ui.changePw;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.databinding.FragmentChangePwBinding;

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
 * 비밀번호 변경 Fragment
 *
 * 주요 기능:
 *   - 새 비밀번호 / 비밀번호 확인 실시간 일치 여부 표시 (TextWatcher)
 *   - 일치하는 경우에만 변경 버튼 활성화 (pwCheckResult)
 *   - 변경 성공 시 SharedPreferences의 user_pw 갱신 (자동 로그인 유지)
 *
 * 서버 cmd: "change_pw"
 *   전송: no (user_no), pw (새 비밀번호)
 *   응답: {"result":"true"} 또는 {"result":"false"}
 */
public class ChangePwFragment extends Fragment {

    private FragmentChangePwBinding binding;

    private EditText  editPw;      // 새 비밀번호 입력
    private EditText  editPwCheck; // 새 비밀번호 확인 입력
    private TextView  tvPwCheck;   // 비밀번호 일치 여부 안내 메시지
    private Button    btnChange;   // 변경 버튼

    private SharedPreferences sharedPreferences;
    private String  userNo        = ""; // 내 user_no
    private String  serverUrl     = "";
    private boolean pwCheckResult = false; // 비밀번호 일치 여부 (일치 시에만 changePw 호출)

    // 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChangePwViewModel changePwViewModel =
                new ViewModelProvider(this).get(ChangePwViewModel.class);

        binding = FragmentChangePwBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_user);

        // Fragment 생명주기 안전 처리
        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // View 초기화
        editPw      = root.findViewById(R.id.changePwFragment_editText_pw);
        editPwCheck = root.findViewById(R.id.changePwFragment_editText_chk_pw);
        tvPwCheck   = root.findViewById(R.id.changePwFragment_textView_check);
        btnChange   = root.findViewById(R.id.changePwFragment_button_change);

        // 변경 버튼 → 입력값 검증 후 changePw() 호출
        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("".equals(editPw.getText().toString())) {
                    Toast.makeText(getContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("".equals(editPwCheck.getText().toString())) {
                    Toast.makeText(getContext(), "비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 비밀번호가 일치하는 경우에만 변경 요청
                if (pwCheckResult) {
                    changePw();
                }
            }
        });

        // 새 비밀번호 실시간 일치 확인 (editPw 변경 시)
        editPw.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Activity act = getActivity();
                if (act == null) return;

                String pw      = editPw.getText().toString();
                String pwCheck = editPwCheck.getText().toString();

                if (!pw.equals(pwCheck)) {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(act, R.color.color_r));
                    pwCheckResult = false;
                } else {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(act, R.color.theme_color));
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
                Activity act = getActivity();
                if (act == null) return;

                String pw      = editPw.getText().toString();
                String pwCheck = editPwCheck.getText().toString();

                if (!pw.equals(pwCheck)) {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(act, R.color.color_r));
                    pwCheckResult = false;
                } else {
                    tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치합니다.");
                    tvPwCheck.setTextColor(ContextCompat.getColor(act, R.color.theme_color));
                    pwCheckResult = true;
                }
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        return root;
    }

    /**
     * 비밀번호 변경 서버 요청
     * - cmd: "change_pw", no (user_no), pw (새 비밀번호) 전송
     * - 성공 시: SharedPreferences의 user_pw를 새 비밀번호로 갱신 (자동 로그인 유지)
     * - 실패 시: 안내 토스트
     */
    private void changePw() {
        Log.i("HS ChangePwFragment", "change pw start");

        String pw = editPw.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "change_pw")
                .add("no",  userNo)
                .add("pw",  pw)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS ChangePwFragment", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // IO 작업은 runOnUiThread 밖에서 처리
                if (response.body() == null) return;
                final String responseData = response.body().string();

                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("HS", "응답 성공");

                            JSONObject json   = new JSONObject(responseData);
                            String     result = json.getString("result");

                            if ("true".equals(result)) {
                                // 변경 성공 → SharedPreferences의 비밀번호 갱신 (자동 로그인 유지)
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_no", userNo);
                                editor.putString("user_pw", pw);
                                editor.apply();

                                Toast.makeText(getContext(), "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // ViewBinding 참조 해제 (메모리 누수 방지)
    }
}
