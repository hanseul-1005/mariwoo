package com.windy.mariwoo.basic.ui.changePw;

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

import com.windy.mariwoo.R;
import com.windy.mariwoo.databinding.FragmentChangePwBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChangePwFragment extends Fragment {

    private FragmentChangePwBinding binding;
    private EditText editPw;
    private EditText editPwCheck;
    private TextView tvPwCheck;
    private Button btnChange;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";
    private boolean pwCheckResult = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChangePwViewModel changePwViewModel =
                new ViewModelProvider(this).get(ChangePwViewModel.class);

        binding = FragmentChangePwBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_user);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        editPw = root.findViewById(R.id.changePwFragment_editText_pw);
        editPwCheck = root.findViewById(R.id.changePwFragment_editText_chk_pw);
        tvPwCheck = root.findViewById(R.id.changePwFragment_textView_check);

        btnChange = root.findViewById(R.id.changePwFragment_button_change);

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if("".equals(editPw.getText().toString())) {
                    Toast.makeText(getContext(), "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if("".equals(editPwCheck.getText().toString())) {
                    Toast.makeText(getContext(), "비밀번호 확인을 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(pwCheckResult) {
                    changePw();
                }
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!pw.equals(pwCheck)) {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");

                            // MainActivity.this 처럼 Activity 이름을 명시해야 Context로 인식됩니다.
                            int textColor = ContextCompat.getColor(getActivity(), R.color.color_r);
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!pw.equals(pwCheck)) {
                            tvPwCheck.setText("비밀번호와 비밀번호 확인이 일치하지않습니다.");

                            // MainActivity.this 처럼 Activity 이름을 명시해야 Context로 인식됩니다.
                            int textColor = ContextCompat.getColor(getActivity(), R.color.color_r);
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
        return root;
    }


    private void changePw() {
        Log.i("HS ChangePwActivity", "change pw start");

        String pw = editPw.getText().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "change_pw")
                .add("no", userNo)
                .add("pw", pw)
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS ChangePwActivity", "request : "+request.toString());
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "응답 성공");
                            final String responseData = response.body().string();

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {
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
        binding = null;
    }
}