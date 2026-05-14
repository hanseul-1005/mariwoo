package kr.windy.mariwoo.basic.ui.medicineList;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.activity.MedicineAddActivity;
import kr.windy.mariwoo.basic.activity.MedicineModifyActivity;
import kr.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import kr.windy.mariwoo.basic.model.MedicineModel;
import kr.windy.mariwoo.databinding.FragmentMedicineListBinding;

import org.json.JSONArray;
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

/**
 * 약 목록 Fragment
 * - 요일 스피너 선택 시 해당 요일의 약 목록 조회
 * - 약 카드 클릭 시 MedicineModifyActivity로 이동
 * - 추가 버튼 클릭 시 MedicineAddActivity로 이동
 * - 추가/수정 완료 후 돌아오면 목록 자동 새로고침
 */
public class MedicineListFragment extends Fragment {

    private FragmentMedicineListBinding binding;
    private Spinner          spinnerWeekday;
    private AppCompatButton  btnAdd;

    // 약 목록 데이터 및 어댑터
    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel>  listMedicineName;

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";

    // 싱글톤 OkHttpClient (매 요청마다 생성하지 않고 재사용)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMedicineListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView rvOuterRecyclerView = binding.medicineListFragmentRecyclerviewOuter;

        serverUrl = getString(R.string.server_medicine);

        // Fragment 생명주기 안전 처리: getActivity() null 체크
        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // 요일 스피너 설정
        spinnerWeekday = root.findViewById(R.id.medicineListFragment_spinner_weekday);
        String[] weekdays = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_item,
                weekdays
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWeekday.setAdapter(adapter);

        // 오늘 요일 자동 선택 (리스너 등록 전에 설정해야 초기 호출 방지 가능)
        // Calendar.DAY_OF_WEEK: 1=일 ~ 7=토  →  앱 기준: 0=월 ~ 6=일
        int dayOfWeek  = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int todayIndex = (dayOfWeek + 5) % 7; // 일=0 → 변환식으로 월(0) 기준 인덱스 계산
        spinnerWeekday.setSelection(todayIndex);

        // 요일 선택 시 해당 요일 약 목록 조회
        spinnerWeekday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getList(); // 선택된 요일로 목록 재조회
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        listMedicineName = new ArrayList<>();

        // 약 카드 클릭 → 수정 화면 이동
        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicineName, medicine -> {
            int weekDay = spinnerWeekday.getSelectedItemPosition(); // 현재 선택된 요일

            Activity act = getActivity();
            if (act == null) return;
            Intent intent = new Intent(act, MedicineModifyActivity.class);
            intent.putExtra("medicine_no",   medicine.getMedicineNo()); // 약 번호 전달
            intent.putExtra("medicine_name", medicine.getName());       // 약 이름 전달
            intent.putExtra("weekday",       weekDay);                  // 선택 요일 전달
            activityResultLauncher.launch(intent);
        });
        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);

        // 약 추가 버튼
        btnAdd = root.findViewById(R.id.medicineListFragment_button_add);
        btnAdd.setOnClickListener(view -> {
            Activity act = getActivity();
            if (act == null) return;
            Intent intent = new Intent(act, MedicineAddActivity.class);
            activityResultLauncher.launch(intent);
        });

        return root;
    }

    /**
     * MedicineAddActivity / MedicineModifyActivity에서 돌아왔을 때 목록 새로고침
     * RESULT_OK인 경우에만 재조회 (취소 시 불필요한 네트워크 요청 방지)
     */
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    getList();
                }
            }
    );

    /**
     * 서버에서 약 목록 조회
     * 현재 선택된 요일(spinnerWeekday)에 해당하는 약만 조회
     */
    private void getList() {
        Log.i("HS MedicineListFragment", "get list");

        int weekDay = spinnerWeekday.getSelectedItemPosition(); // 0:월 ~ 6:일

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",      "list")
                .add("no",       userNo)
                .add("week_day", String.valueOf(weekDay))
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        Log.i("HS ListFragment", "request : " + request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // body 읽기는 백그라운드 스레드에서 처리 (IO on main thread 방지)
                if (response.body() == null) return;
                final String responseData = response.body().string();

                // Fragment가 detach된 상태면 UI 업데이트 중단
                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        Log.i("HS", "ListFragment 응답 성공");

                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            JSONArray jArr = json.getJSONArray("listName");
                            listMedicineName.clear();

                            for (int i = 0; i < jArr.length(); i++) {
                                JSONObject nameObj = jArr.getJSONObject(i);
                                String     name    = nameObj.getString("name");

                                MedicineModel medicine = new MedicineModel();
                                medicine.setName(name);
                                medicine.setMedicineNo(nameObj.getString("medicine_no")); // 수정 화면 이동에 사용

                                Log.i("HS", "medicine_no : " + nameObj.getString("medicine_no"));

                                // 스케줄 목록 파싱 (아침/점심/저녁/취침 전)
                                JSONArray         jArr2       = nameObj.getJSONArray("listMedicine");
                                List<MedicineModel> listMedicine = new ArrayList<>();

                                for (int j = 0; j < jArr2.length(); j++) {
                                    JSONObject detailObj = jArr2.getJSONObject(j);
                                    String     no        = detailObj.getString("schedule_no");
                                    String     intakeTime = detailObj.getString("intake_time");      // 식전/식후 + 시간
                                    String     intakeType = detailObj.getString("intake_time_type"); // 아침/점심 등

                                    MedicineModel listModel = new MedicineModel();
                                    listModel.setType(intakeType);
                                    listModel.setTime(intakeTime);
                                    listModel.setNo(no);
                                    listMedicine.add(listModel);
                                }

                                medicine.setListMedicine(listMedicine);
                                listMedicineName.add(medicine);
                            }

                            // 파싱 완료 후 어댑터 갱신
                            if (outerAdapter != null) {
                                outerAdapter.notifyDataSetChanged();
                            }

                        } else {
                            Toast.makeText(getContext(),
                                    "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
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
