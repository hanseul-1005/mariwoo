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
import android.widget.LinearLayout;
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

        AppCompatButton btnAdd = binding.medicineListFragmentButtonAdd;

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MedicineAddActivity.class);
                activityResultLauncher.launch(intent);
            }
        });

        // 수정
        /*LinearLayout layoutModify = binding.medicineListFragmentLayoutSearch;
        layoutModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MedicineModifyActivity.class);
                activityResultLauncher.launch(intent);
            }
        });*/



        // 예시 데이터 세팅
        listMedicineName = new ArrayList<>();
/*

        MedicineModel medicine = new MedicineModel();
        medicine.setName("고혈압약");

        List<MedicineModel> listMedicine = new ArrayList<>();

        MedicineModel listModel1 = new MedicineModel();
        listModel1.setType("아침");
        listModel1.setTime("식전 07:00");
        listModel1.setNo("1");
        listMedicine.add(listModel1);

        MedicineModel listModel2 = new MedicineModel();
        listModel2.setType("점심");
        listModel2.setTime("식전 13:00");
        listModel2.setNo("1");
        listMedicine.add(listModel2);

        medicine.setListMedicine(listMedicine);

        listMedicineName.add(medicine);

        medicine = new MedicineModel();
        medicine.setName("당뇨");

        listMedicine = new ArrayList<>();

        MedicineModel listModel3 = new MedicineModel();
        listModel3.setType("아침");
        listModel3.setTime("식전 08:00");
        listModel3.setNo("2");
        listMedicine.add(listModel3);

        medicine.setListMedicine(listMedicine);
        listMedicineName.add(medicine);

        Log.d("HS", "listMedicineName size : "+listMedicineName.size());
*/

        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicineName);
        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);



        return root;
    }




    // 약 알람 선택 후 해당 값 가져오기
    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {

            if(result.getResultCode() == RESULT_OK) {
                Intent resultIntent = result.getData();
                String state = resultIntent.getStringExtra("state");

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d("HS", "retrun");
                    }
                });
            }
        }
    });


    private void getList() {
        Log.i("HS MedicineListFragment", "get list");



        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "list")
                .add("userNo", userNo)
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

                // 서브 스레드 Ui 변경 할 경우 에러
                // 메인스레드 Ui 설정
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            Log.i("HS", "ListFragment 응답 성공");
                            final String responseData = response.body().string();

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");

                            if("true".equals(result)) {

                                JSONArray jArr = json.getJSONArray("listName");

                                String medicineName = "";

                                for(int i=0; i<jArr.length(); i++) {
                                    String name = jArr.getJSONObject(i).getString("name");

                                    MedicineModel medicine = new MedicineModel();
                                    medicine.setName(name);

                                    JSONArray jArr2 = jArr.getJSONObject(i).getJSONArray("listMedicine");

                                    for(int j=0; j<jArr2.length(); j++) {

                                        String no = jArr2.getJSONObject(i).getString("no");
                                        String intakeTime = jArr2.getJSONObject(i).getString("intake_time");
                                        String intakeType = jArr2.getJSONObject(i).getString("intake_time_type");

                                        List<MedicineModel> listMedicine = new ArrayList<>();

                                        MedicineModel listModel = new MedicineModel();
                                        listModel.setType(intakeType);
                                        listModel.setTime(intakeTime);
                                        listModel.setNo(no);
                                        listMedicine.add(listModel);

                                        medicine.setListMedicine(listMedicine);

                                    }

                                    listMedicineName.add(medicine);

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