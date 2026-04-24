package com.windy.mariwoo.basic.ui.changeInfo;

import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.windy.mariwoo.R;
import com.windy.mariwoo.databinding.FragmentChangeInfoBinding;

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

public class ChangeInfoFragment extends Fragment {

    private FragmentChangeInfoBinding binding;
    private EditText editTel;
    private TextInputEditText editEmail;
    private AutoCompleteTextView editAddr;
    private TextInputLayout layoutEmailDirect;
    private TextInputEditText editAddrDirect;
    private TextInputEditText editBirth;



    private final String[] emailDomains = {
            "gmail.com",
            "naver.com",
            "hanmail.net",
            "kakao.com",
            "outlook.com",
            "hotmail.com",
            "직접입력"
    };

    private ChangeInfoViewModel mViewModel;
    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String userTel = "";
    private String userEmail = "";
    private String userBirth = "";
    private String serverUrl = "";

    public static ChangeInfoFragment newInstance() {
        return new ChangeInfoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ChangeInfoViewModel changeInfoViewModel =
                new ViewModelProvider(this).get(ChangeInfoViewModel.class);

        binding = FragmentChangeInfoBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        serverUrl = getString(R.string.server_user);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");
        userTel = sharedPreferences.getString("user_tel", "");
        userEmail = sharedPreferences.getString("user_email", "");
        userBirth = sharedPreferences.getString("user_birth", "");

        editTel = root.findViewById(R.id.changeInfoFragment_editText_tel);
        editEmail = root.findViewById(R.id.changeInfoFragment_textField_email);
        editAddr = root.findViewById(R.id.changeInfoFragment_autoTextview_emailAddr);
        layoutEmailDirect = root.findViewById(R.id.changeInfoFragment_textInputLayout_emailAddr);
        editAddrDirect = root.findViewById(R.id.changeInfoFragment_textInput_emailAddr);
        editBirth = root.findViewById(R.id.changeInfoFragment_editText_birth);

        editBirth.setOnClickListener(v -> showBirthDatePicker());
        editBirth.setTextColor(getResources().getColor(android.R.color.black));
        String[] parts = userEmail.split("@");

        String emailId = parts[0]; // "test"
        String emailAddr = parts[1];   // "gmail.com"
        int idx = 6;

        for(int i=0; i<emailDomains.length-1; i++) {
            if(emailDomains[i].equals(emailAddr)) {
                idx=i;
            }
        }

        editTel.setText(userTel);
        editEmail.setText(emailId);
        editBirth.setText(userBirth);

        editAddr.setText(emailDomains[idx], false);
        if(idx==6) {
            editAddrDirect.setText(emailAddr);
        }

        editAddr.setOnItemClickListener((parent, view, position, id) -> {
            String selected = editAddr.getText().toString();

            if ("직접입력".equals(selected)) {
                layoutEmailDirect.setVisibility(View.VISIBLE);
                editAddrDirect.requestFocus();
            } else {
                layoutEmailDirect.setVisibility(View.GONE);
                editAddrDirect.setText("");
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

        return root;
    }


    private void changeInfo() {
        Log.i("HS ChangeInfoFragment", "change info start");

        userTel = editTel.getText().toString();
        String emailId = editEmail.getText().toString();
        String emailAddr = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();

        if("직접 입력".equals(emailAddr)) {
            userEmail = emailId+emailAddrDirect;
        } else {
            userEmail = emailId+emailAddr;
        }
        userBirth = editBirth.getText().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "change_info")
                .add("userNo", userNo)
                .add("tel", userTel)
                .add("email", userEmail)
                .add("birth", userBirth)
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
                                editor.putString("user_tel", userTel);
                                editor.putString("user_email", userEmail);
                                editor.putString("user_birth", userBirth);

                                editor.apply();

                                Toast.makeText(getContext(), "내 정보가 변경되었습니다.", Toast.LENGTH_SHORT).show();
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

    private void showBirthDatePicker() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        utc.set(1920, Calendar.JANUARY, 1);
        long startDate = utc.getTimeInMillis();

        long endDate = MaterialDatePicker.todayInUtcMilliseconds();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        long openAt = endDate; // 기본값
        try {
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            openAt = sdf.parse(userBirth).getTime();
        } catch (Exception e) {
            e.printStackTrace();
        }

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(endDate)
                .setOpenAt(openAt)
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("생년월일 선택")
                .setSelection(openAt)
                .setCalendarConstraints(constraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            String birth = sdf.format(new Date(selection));
            editBirth.setText(birth);
        });

        picker.show(getChildFragmentManager(), "birth_picker");
    }
}