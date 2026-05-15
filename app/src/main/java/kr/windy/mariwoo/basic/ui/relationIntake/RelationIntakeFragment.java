package kr.windy.mariwoo.basic.ui.relationIntake;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import kr.windy.mariwoo.basic.model.MedicineModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 가족 복용 내역 조회 Fragment (열람자 화면)
 * - 스피너에서 가족(대상자) 선택
 * - 날짜 선택 후 조회 버튼 → 해당 날짜의 복용 현황 표시
 * - 복용 체크는 불가 (열람 전용)
 *
 * 동작 흐름:
 *   1. onCreateView → loadFamilyList(): 내가 열람할 수 있는 가족 목록 불러오기
 *   2. 조회 버튼 → getIntakeList(): 선택한 가족의 복용 현황 조회
 */
public class RelationIntakeFragment extends Fragment {

    private Spinner           spinnerTarget; // 가족(대상자) 선택 스피너
    private TextInputEditText editDate;      // 날짜 입력 필드
    private AppCompatButton   btnSelect;     // 조회 버튼
    private RecyclerView      rvOuter;       // 약 목록 RecyclerView

    // 약 목록 어댑터 및 데이터
    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel>  listMedicine = new ArrayList<>();

    // 스피너에 표시할 가족 이름 목록 및 대응하는 user_no 목록
    // 인덱스가 일치해야 함 (familyNames[i] ↔ familyNos[i])
    private List<String> familyNames = new ArrayList<>();
    private List<String> familyNos   = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";

    // 싱글톤 OkHttpClient (loadFamilyList, getIntakeList 공용)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_relation_intake, container, false);

        serverUrl = getString(R.string.server_medicine);

        // Fragment 생명주기 안전 처리
        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // View 초기화
        spinnerTarget = root.findViewById(R.id.relationIntakeFragment_spinner_target);
        editDate      = root.findViewById(R.id.relationIntakeFragment_editText_date);
        btnSelect     = root.findViewById(R.id.relationIntakeFragment_button_select);
        rvOuter       = root.findViewById(R.id.relationIntakeFragment_recyclerview_outer);

        // 오늘 날짜를 기본값으로 설정
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        editDate.setText(today);

        // 날짜 필드 클릭 → DatePickerDialog 표시
        editDate.setOnClickListener(v -> showDatePicker());

        // RecyclerView 설정 (열람 전용이므로 클릭 콜백 null)
        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicine, null);
        rvOuter.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuter.setAdapter(outerAdapter);

        // 조회 버튼 클릭 → 복용 내역 조회
        btnSelect.setOnClickListener(v -> getIntakeList());

        // 화면 진입 시 가족 목록 먼저 로드 (스피너 데이터)
        loadFamilyList();

        return root;
    }

    /**
     * 날짜 선택 다이얼로그
     * editDate 현재값을 초기 날짜로 표시
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            android.text.Editable editable = editDate.getText();
            if (editable != null && !editable.toString().isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                calendar.setTime(sdf.parse(editable.toString()));
            }
        } catch (Exception ignored) {}

        Activity act = getActivity();
        if (act == null) return;

        new DatePickerDialog(act, (view, y, m, d) -> {
            // 선택한 날짜를 yyyy-MM-dd 형식으로 editDate에 설정
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            editDate.setText(date);
        },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * 날짜 문자열 → 요일 인덱스 변환
     * @return 0:월 ~ 6:일
     */
    private int getWeekdayFromDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=일 ~ 7=토
            return (dayOfWeek + 5) % 7;                    // 0=월 ~ 6=일 로 변환
        } catch (Exception e) {
            return 0; // 파싱 실패 시 월요일 기본값
        }
    }

    /**
     * 내가 열람할 수 있는 가족(대상자) 목록 조회
     * 서버에서 받은 목록으로 스피너 구성
     * familyNames와 familyNos 인덱스 동기화 필수
     */
    private void loadFamilyList() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd",       "relation_list")
                .add("no",        userNo)   // 내 user_no
                .add("target_no", userNo)   // 가족 목록 조회용 (서버 처리 방식에 따라 다름)
                .add("date",      editDate.getText() != null ? editDate.getText().toString() : "")
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
                    String    responseData = response.body().string();
                    Log.i("HS", "가족목록 응답 : " + responseData);

                    JSONObject json     = new JSONObject(responseData);
                    JSONArray  jArrUser = json.getJSONArray("listTarget");

                    // 기존 목록 초기화 후 재구성
                    familyNames.clear();
                    familyNos.clear();

                    for (int i = 0; i < jArrUser.length(); i++) {
                        JSONObject obj = jArrUser.getJSONObject(i);
                        familyNames.add(obj.getString("name"));              // 스피너 표시용 이름
                        familyNos.add(String.valueOf(obj.getLong("no")));    // 조회 시 사용할 user_no
                    }

                    // UI 업데이트는 메인 스레드에서
                    Activity uiAct = getActivity();
                    if (uiAct == null || uiAct.isFinishing()) return;
                    uiAct.runOnUiThread(() -> {
                        if (familyNames.isEmpty()) {
                            Toast.makeText(getContext(), "등록된 가족이 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // 스피너에 가족 이름 목록 설정
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                uiAct,
                                android.R.layout.simple_spinner_item,
                                familyNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerTarget.setAdapter(adapter);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 선택한 가족의 복용 내역 조회
     * - 스피너 선택 인덱스로 familyNos에서 target_no 추출
     * - 선택한 날짜와 요일을 함께 전송
     */
    private void getIntakeList() {
        if (familyNos.isEmpty()) {
            Toast.makeText(getContext(), "가족을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 스피너 선택 인덱스 범위 체크 (IndexOutOfBoundsException 방지)
        int selectedIndex = spinnerTarget.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= familyNos.size()) return;

        String targetNo = familyNos.get(selectedIndex); // 대상자 user_no
        String date     = editDate.getText() != null ? editDate.getText().toString() : "";
        int    weekday  = getWeekdayFromDate(date);      // 날짜 → 요일 변환

        Log.i("HS", "조회 targetNo=" + targetNo + " date=" + date + " weekday=" + weekday);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",       "get_family_intake_list") // 가족 복용 내역 조회 API
                .add("target_no", targetNo)                 // 대상자 번호
                .add("date",      date)
                .add("weekday",   String.valueOf(weekday))
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
                    String    responseData = response.body().string();
                    Log.i("HS", "복약내역 응답 : " + responseData);

                    // 응답은 JSONArray 형태: [{medicine_name, schedules:[...]}, ...]
                    JSONArray           jArr     = new JSONArray(responseData);
                    List<MedicineModel> tempList = new ArrayList<>();

                    for (int i = 0; i < jArr.length(); i++) {
                        JSONObject    cardObj  = jArr.getJSONObject(i);
                        MedicineModel medicine = new MedicineModel();
                        medicine.setName(cardObj.getString("medicine_name"));

                        JSONArray           schedules = cardObj.getJSONArray("schedules");
                        List<MedicineModel> innerList = new ArrayList<>();

                        for (int j = 0; j < schedules.length(); j++) {
                            JSONObject    s     = schedules.getJSONObject(j);
                            MedicineModel inner = new MedicineModel();
                            inner.setNo(s.getString("schedule_no"));
                            inner.setType(s.getString("intake_time_type")); // 아침/점심 등
                            inner.setTime(s.getString("intake_time"));      // HH:mm
                            inner.setIntakeYn(String.valueOf(s.getInt("is_taken"))); // 1=복용, 0=미복용

                            Log.i("HS", "is_taken=" + s.getInt("is_taken")
                                    + " intakeYn=" + inner.getIntakeYn());
                            innerList.add(inner);
                        }

                        medicine.setListMedicine(innerList);
                        tempList.add(medicine);
                    }

                    // UI 업데이트
                    Activity uiAct = getActivity();
                    if (uiAct == null || uiAct.isFinishing()) return;
                    uiAct.runOnUiThread(() -> {
                        listMedicine.clear();
                        listMedicine.addAll(tempList);
                        outerAdapter.notifyDataSetChanged();

                        if (listMedicine.isEmpty()) {
                            Toast.makeText(getContext(), "복약 내역이 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
