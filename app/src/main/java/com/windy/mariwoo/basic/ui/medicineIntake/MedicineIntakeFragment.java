package com.windy.mariwoo.basic.ui.medicineIntake;

import static android.app.Activity.RESULT_OK;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.windy.mariwoo.basic.activity.DatePickerActivity;
import com.windy.mariwoo.basic.activity.MedicineAddActivity;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.databinding.FragmentMedicineIntakeBinding;

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

public class MedicineIntakeFragment extends Fragment {

    private FragmentMedicineIntakeBinding binding;

    // 목록
    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel> listMedicineName;

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        MedicineIntakeViewModel medicineIntakeViewModel =
                new ViewModelProvider(this).get(MedicineIntakeViewModel.class);

        binding = FragmentMedicineIntakeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayout layoutCalendar = binding.medicineIntakeFragmentLayoutCalendar;
        layoutCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), DatePickerActivity.class);
                activityResultLauncher.launch(intent);
            }
        });

        LinearLayout layoutCheck = binding.medicineIntakeFragmentLayoutMorningCheck;
        layoutCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder msgBuilder = new AlertDialog.Builder(getContext())
                        .setTitle("약 알람")
                        .setMessage("약 드셨어요?")
                        .setPositiveButton("예", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        })
                        .setNeutralButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });

                AlertDialog msgDlg = msgBuilder.create();
                msgDlg.show();
            }
        });

        return root;
    }

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
        Log.i("HS MedicineIntakeFragment", "get list");


        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "login")
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