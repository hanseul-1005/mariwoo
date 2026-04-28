package com.windy.mariwoo.basic.ui.relationIntake;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.basic.model.UserModel;
import com.windy.mariwoo.databinding.FragmentMedicineListBinding;
import com.windy.mariwoo.databinding.FragmentRelationIntakeBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RelationIntakeFragment extends Fragment {

    private FragmentRelationIntakeBinding binding;
    private Spinner spinnerTarget;
    private TextInputEditText editDate;
    private AppCompatButton btnSelect;

    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel> listMedicineName = new ArrayList<>();
    private List<UserModel> listTarget = new ArrayList<>();

    private ArrayAdapter<UserModel> spinnerTargetAdapter;


    private SharedPreferences sharedPreferences;
    private String userNo = "-1";
    private String targetNo = "-1";
    private String serverUrl = "";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        RelationIntakeViewModel relationIntakeViewModel =
                new ViewModelProvider(this).get(RelationIntakeViewModel.class);

        binding = FragmentRelationIntakeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 목록
        RecyclerView rvOuterRecyclerView = binding.relationIntakeFragmentRecyclerviewOuter;

        serverUrl = getString(R.string.server_medicine);

        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");


        editDate = root.findViewById(R.id.relationIntakeFragment_editText_date);
        editDate.setOnClickListener(v -> showBirthDatePicker());
        editDate.setTextColor(getResources().getColor(android.R.color.black));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
        String today = sdf.format(new Date());
        editDate.setText(today);

        // 대상자 Spinner 세팅
        spinnerTarget = binding.relationIntakeFragmentSpinnerTarget; // 실제 id로 변경
        spinnerTargetAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listTarget
        );
        spinnerTargetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTarget.setAdapter(spinnerTargetAdapter);

        // 대상자 선택 시 no값 꺼내기
        spinnerTarget.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (listTarget.isEmpty()) return;

                UserModel selectedUser = (UserModel) parent.getItemAtPosition(position);
                targetNo = selectedUser.getNo();

                // 필요하다면 여기서 selectedNo로 추가 조회 가능
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // RecyclerView 세팅
        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicineName);
        rvOuterRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuterRecyclerView.setAdapter(outerAdapter);

        // 화면 실행 시 최초 조회
        getList();

        btnSelect = root.findViewById(R.id.relationIntakeFragment_button_select);
        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getList();
            }
        });

        return root;
    }

    private void getList() {
        Log.i("HS RelationIntakeFragment", "get list");

        String date = editDate.getText().toString();

        // POST 파라미터 추가
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "relation_list")
                .add("no", userNo)
                .add("target_no", targetNo)
                .add("date", date)
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
                final String responseData = response.body().string();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            JSONObject json = new JSONObject(responseData);

                            String result = json.getString("result");
                            Log.i("HS", "ListFragment 응답 성공 result : "+result);

                            if("true".equals(result)) {


                                // ── listMedicine 파싱 ──
                                listMedicineName.clear();
                                JSONArray jArr = json.getJSONArray("listName");
                                for (int i = 0; i < jArr.length(); i++) {
                                    JSONObject nameObj = jArr.getJSONObject(i);
                                    MedicineModel medicine = new MedicineModel();
                                    medicine.setName(nameObj.getString("name"));

                                    List<MedicineModel> listMedicine = new ArrayList<>();
                                    JSONArray jArr2 = nameObj.getJSONArray("listMedicine");
                                    for (int j = 0; j < jArr2.length(); j++) {
                                        JSONObject detailObj = jArr2.getJSONObject(j);
                                        MedicineModel m = new MedicineModel();
                                        m.setNo(detailObj.getString("schedule_no"));
                                        m.setTime(detailObj.getString("intake_time"));
                                        m.setType(detailObj.getString("intake_time_type"));
                                        listMedicine.add(m);
                                    }
                                    medicine.setListMedicine(listMedicine);
                                    listMedicineName.add(medicine);
                                }
                                outerAdapter.notifyDataSetChanged();

                                // ── listTarget 파싱 후 Spinner 갱신 ──
                                listTarget.clear();
                                JSONArray jArr3 = json.getJSONArray("listTarget");
                                for (int k = 0; k < jArr3.length(); k++) {
                                    JSONObject obj = jArr3.getJSONObject(k);
                                    UserModel user = new UserModel();
                                    user.setNo(obj.getString("no"));
                                    user.setName(obj.getString("name"));
                                    listTarget.add(user);
                                }
                                spinnerTargetAdapter.notifyDataSetChanged();

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

    private void showBirthDatePicker() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        utc.set(1920, Calendar.JANUARY, 1);
        long startDate = utc.getTimeInMillis();

        long endDate = MaterialDatePicker.todayInUtcMilliseconds();

        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setStart(startDate)
                .setEnd(endDate)
                .setOpenAt(endDate)
                .setValidator(DateValidatorPointBackward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("생년월일 선택")
                .setSelection(endDate)
                .setCalendarConstraints(constraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.KOREA);
            String birth = sdf.format(new Date(selection));
            editDate.setText(birth);
        });

        picker.show(getChildFragmentManager(), "date_picker");
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}