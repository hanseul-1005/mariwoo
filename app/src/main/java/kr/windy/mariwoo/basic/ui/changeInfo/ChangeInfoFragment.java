package kr.windy.mariwoo.basic.ui.changeInfo;

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
import android.widget.ArrayAdapter;
import android.widget.Filter;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import kr.windy.mariwoo.R;
import kr.windy.mariwoo.databinding.FragmentChangeInfoBinding;

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
 * 내 정보 변경 Fragment
 *
 * 변경 가능 항목:
 *   - 전화번호 (자동 하이픈 포맷: 000-0000-0000)
 *   - 이메일 (도메인 드롭다운 + "직접입력" 지원)
 *   - 생년월일 (MaterialDatePicker)
 *
 * 동작 흐름:
 *   1. onCreateView → SharedPreferences에서 현재 정보 로드 후 화면에 표시
 *   2. 변경 버튼(changeInfo()) → 서버에 업데이트 요청
 *   3. 성공 시 SharedPreferences 갱신 및 안내 토스트
 *
 * 서버 cmd: "change_info"
 *   전송: userNo, tel, email, birth
 */
public class ChangeInfoFragment extends Fragment {

    private FragmentChangeInfoBinding binding;

    private EditText          editTel;          // 전화번호 입력 (자동 하이픈)
    private TextInputEditText editEmail;        // 이메일 아이디 부분
    private MaterialAutoCompleteTextView editAddr; // 이메일 도메인 드롭다운
    private TextInputLayout   layoutEmailDirect; // "직접입력" 선택 시 표시되는 레이아웃
    private TextInputEditText editAddrDirect;  // 도메인 직접 입력 필드
    private TextInputEditText editBirth;        // 생년월일

    // 이메일 도메인 선택 목록 (마지막 항목 "직접입력" 선택 시 추가 입력 표시)
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

    private String userNo    = ""; // 내 user_no
    private String userTel   = ""; // 현재 저장된 전화번호
    private String userEmail = ""; // 현재 저장된 이메일 (전체: id@domain)
    private String userBirth = ""; // 현재 저장된 생년월일
    private String serverUrl = "";

    // 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

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

        // Fragment 생명주기 안전 처리
        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);

        // SharedPreferences에서 현재 사용자 정보 로드
        userNo    = sharedPreferences.getString("user_no",    "-1");
        userTel   = sharedPreferences.getString("user_tel",   "");
        userEmail = sharedPreferences.getString("user_email", "");
        userBirth = sharedPreferences.getString("user_birth", "");

        // View 초기화
        editTel          = root.findViewById(R.id.changeInfoFragment_editText_tel);
        editEmail        = root.findViewById(R.id.changeInfoFragment_textField_email);
        editAddr         = root.findViewById(R.id.changeInfoFragment_autoTextview_emailAddr);
        layoutEmailDirect = root.findViewById(R.id.changeInfoFragment_textInputLayout_emailAddr);
        editAddrDirect   = root.findViewById(R.id.changeInfoFragment_textInput_emailAddr);
        editBirth        = root.findViewById(R.id.changeInfoFragment_editText_birth);

        // 생년월일 클릭 → MaterialDatePicker
        editBirth.setOnClickListener(v -> showBirthDatePicker());
        editBirth.setTextColor(getResources().getColor(android.R.color.black));

        // 이메일 파싱: "user@gmail.com" → emailId="user", emailAddr="gmail.com"
        String[] parts    = userEmail.split("@");
        String   emailId  = parts.length > 0 ? parts[0] : "";
        String   emailAddr = parts.length > 1 ? parts[1] : "";

        // 저장된 도메인이 목록에 있는지 확인 (없으면 "직접입력" 인덱스 = 6)
        int idx = 6;
        for (int i = 0; i < emailDomains.length - 1; i++) {
            if (emailDomains[i].equals(emailAddr)) {
                idx = i;
            }
        }

        Log.d("HS", "userEmail : " + userEmail);

        // 현재 정보로 화면 초기 설정
        editTel.setText(userTel);
        editEmail.setText(emailId);
        editBirth.setText(userBirth);
        editAddr.setText(emailDomains[idx], false);

        // "직접입력" 도메인이면 직접 입력 필드 표시 및 도메인 값 설정
        if (idx == 6) {
            layoutEmailDirect.setVisibility(View.VISIBLE);
            editAddrDirect.setText(emailAddr);
        }

        // 이메일 도메인 드롭다운 어댑터 (필터링 비활성화 → 항상 전체 목록 표시)
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                emailDomains
        ) {
            @NonNull
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = emailDomains;
                        results.count  = emailDomains.length;
                        return results;
                    }
                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };
        editAddr.setAdapter(adapter);
        editAddr.setThreshold(0);                                  // 입력 없이도 목록 표시
        editAddr.setOnClickListener(v -> editAddr.showDropDown()); // 클릭 시 바로 드롭다운 오픈
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

        // 변경 버튼 → changeInfo() 호출
        root.findViewById(R.id.changeInfoFragment_button_change).setOnClickListener(v -> changeInfo());

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

        return root;
    }

    /**
     * 내 정보 변경 서버 요청
     * - cmd: "change_info" + userNo, tel, email, birth 전송
     * - 성공 시 SharedPreferences 갱신 및 안내 토스트
     *
     * 이메일 조합:
     *   "직접 입력" 도메인 → emailId + emailAddrDirect
     *   선택 도메인        → emailId + emailAddr
     */
    private void changeInfo() {
        Log.i("HS ChangeInfoFragment", "change info start");

        userTel = editTel.getText().toString();
        String emailId         = editEmail.getText().toString();
        String emailAddr       = editAddr.getText().toString();
        String emailAddrDirect = editAddrDirect.getText().toString();

        if ("직접입력".equals(emailAddr)) {
            userEmail = emailId + "@" + emailAddrDirect;
        } else {
            userEmail = emailId + "@" + emailAddr;
        }
        userBirth = editBirth.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",   "change_info")
                .add("no",    userNo)
                .add("tel",   userTel)
                .add("email", userEmail)
                .add("birth", userBirth)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS ChangeInfoFragment", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Activity _fa = getActivity();
                if (_fa != null) _fa.runOnUiThread(() ->
                    android.widget.Toast.makeText(_fa, "네트워크 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show());
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
                            Log.i("HS ChangeInfoFragment", "응답: " + responseData);

                            // 서버가 HTML 에러 페이지를 반환한 경우
                            if (responseData.trim().startsWith("<")) {
                                Toast.makeText(act, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            JSONObject json   = new JSONObject(responseData);
                            String     result = json.getString("result");

                            if ("true".equals(result)) {
                                // 변경 성공 → SharedPreferences 갱신
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("user_no",    userNo);
                                editor.putString("user_tel",   userTel);
                                editor.putString("user_email", userEmail);
                                editor.putString("user_birth", userBirth);
                                editor.apply();

                                Toast.makeText(act, "내 정보가 변경되었습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(act, "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
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
        binding           = null;
        editTel           = null;
        editEmail         = null;
        editAddr          = null;
        layoutEmailDirect = null;
        editAddrDirect    = null;
        editBirth         = null;
    }

    /**
     * 생년월일 선택 다이얼로그 표시 (MaterialDatePicker)
     * - 1920년 1월 1일 ~ 오늘까지만 선택 가능
     * - 저장된 생년월일을 초기 선택 날짜로 설정
     * - 선택 완료 시 editBirth에 yyyy-MM-dd 형식으로 설정
     */
    private void showBirthDatePicker() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.set(1920, Calendar.JANUARY, 1);
        long startDate = utc.getTimeInMillis();
        long endDate   = MaterialDatePicker.todayInUtcMilliseconds();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

        // 저장된 생년월일을 초기값으로 설정 (파싱 실패 시 오늘)
        long openAt = endDate;
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
                .setValidator(DateValidatorPointBackward.now()) // 오늘 이전만 선택 가능
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

