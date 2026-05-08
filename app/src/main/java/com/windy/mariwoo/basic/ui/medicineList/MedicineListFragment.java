package com.windy.mariwoo.basic.ui.medicineList;

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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.LoginActivity;
import com.windy.mariwoo.basic.MainActivity;
import com.windy.mariwoo.basic.activity.MedicineAddActivity;
import com.windy.mariwoo.basic.activity.MedicineModifyActivity;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.databinding.FragmentMedicineListBinding;

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

public class MedicineListFragment extends Fragment {

    private FragmentMedicineListBinding binding;
    private Spinner spinnerWeekday;
    private AppCompatButton btnAdd;

    // 목록
    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel> listMedicineName;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentMedicineListBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 목록
        View view = inflater.inflate(R.layout.fragment_medicine_list, container, false);
        RecyclerView rvOuterRecyclerView = binding.medicineListFragmentRecyclerviewOuter;

        serverUrl = getString(R.string.server_medicine);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // 1. 스피너 찾기
        spinnerWeekday = root.findViewById(R.id.medicineListFragment_spinner_weekday);

        // 2. 스피너에 넣을 요일 배열 만들기
        String[] weekdays = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
// 3. 어댑터 생성 및 데이터 연결
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getActivity(),
                android.R.layout.simple_spinner_item,
                weekdays
        );

// 4. 드롭다운 레이아웃 설정
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// 5. 어댑터 적용
        spinnerWeekday.setAdapter(adapter);

// ✅ 오늘 요일 자동 선택 (리스너 설정 전에 호출)
        int dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK); // 1=일 ~ 7=토
        int todayIndex = (dayOfWeek + 5) % 7; // 0=월 ~ 6=일
        spinnerWeekday.setSelection(todayIndex);

// 6. 리스너 등록
        spinnerWeekday.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedWeekday = weekdays[position];
                // Toast.makeText(...) // 실서비스에서는 제거 권장
                getList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // 예시 데이터 세팅
        listMedicineName = new ArrayList<>();

        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicineName, medicine -> {
            // 수정 화면으로 이동

            int weekDay = spinnerWeekday.getSelectedItemPosition();

            Intent intent = new Intent(getActivity(), MedicineModifyActivity.class);
            intent.putExtra("medicine_no", medicine.getMedicineNo());
            intent.putExtra("medicine_name", medicine.getName());
            intent.putExtra("weekday", weekDay);
            activityResultLauncher.launch(intent);
        });
        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);

        btnAdd = root.findViewById(R.id.medicineListFragment_button_add);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), MedicineAddActivity.class);
                activityResultLauncher.launch(intent); //
            }
        });

        return root;
    }




    // 약 알람 선택 후 해당 값 가져오기
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        // MedicineAddActivity에서 돌아왔을 때 목록 새로고침
                        getList();
                    }
                }
            }
    );


    private void getList() {
        Log.i("HS MedicineListFragment", "get list");

        int weekDay = spinnerWeekday.getSelectedItemPosition();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "list")
                .add("no", userNo)
                .add("week_day", String.valueOf(weekDay))
                .build();

        // 요청 만들기
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();
        Log.i("HS ListFragment", "request : "+request.toString());
        // 응답 콜백
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (response.body() == null) return;
                final String responseData = response.body().string();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "ListFragment 응답 성공");

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {

                                JSONArray jArr = json.getJSONArray("listName");
                                listMedicineName.clear();

                                for(int i=0; i<jArr.length(); i++) {
                                    JSONObject nameObj = jArr.getJSONObject(i);
                                    String name = nameObj.getString("name");

                                    MedicineModel medicine = new MedicineModel();
                                    medicine.setName(name);
                                    medicine.setMedicineNo(nameObj.getString("medicine_no"));

                                    Log.i("HS", "medicine_no : " + nameObj.getString("medicine_no"));          // ✅ 추가
                                    JSONArray jArr2 = nameObj.getJSONArray("listMedicine");

                                    // 2. 내부 리스트를 안쪽 for문 '밖'에서 한 번만 생성해야 합니다.
                                    List<MedicineModel> listMedicine = new ArrayList<>();

                                    for(int j=0; j<jArr2.length(); j++) {
                                        // 3. jArr2에서 꺼낼 때는 'i'가 아니라 'j'를 사용해야 합니다.
                                        JSONObject detailObj = jArr2.getJSONObject(j);
                                        String no = detailObj.getString("schedule_no");
                                        String intakeTime = detailObj.getString("intake_time");
                                        String intakeType = detailObj.getString("intake_time_type");

                                        MedicineModel listModel = new MedicineModel();
                                        listModel.setType(intakeType);
                                        listModel.setTime(intakeTime);
                                        listModel.setNo(no);

                                        listMedicine.add(listModel);
                                    }

                                    medicine.setListMedicine(listMedicine);
                                    listMedicineName.add(medicine);
                                }

                                // 4. 파싱이 모두 끝난 후 어댑터 새로고침
                                if(outerAdapter != null) {
                                    outerAdapter.notifyDataSetChanged();
                                }


                            } else {
                                Toast.makeText(getContext(), "일치하는 정보가 없습니다.\n입력하신 정보를 확인해주세요." + responseData, Toast.LENGTH_SHORT).show();
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