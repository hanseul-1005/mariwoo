package com.windy.mariwoo.basic.ui.relationIntake;

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
import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineModel;

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

public class RelationIntakeFragment extends Fragment {

    private Spinner          spinnerTarget;
    private TextInputEditText editDate;
    private AppCompatButton  btnSelect;
    private RecyclerView     rvOuter;

    private MedicineOuterAdapter outerAdapter;
    private List<MedicineModel>  listMedicine = new ArrayList<>();

    private List<String> familyNames = new ArrayList<>();
    private List<String> familyNos   = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_relation_intake, container, false);

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = getActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        spinnerTarget = root.findViewById(R.id.relationIntakeFragment_spinner_target);
        editDate      = root.findViewById(R.id.relationIntakeFragment_editText_date);
        btnSelect     = root.findViewById(R.id.relationIntakeFragment_button_select);
        rvOuter       = root.findViewById(R.id.relationIntakeFragment_recyclerview_outer);

        // 오늘 날짜 기본값
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        editDate.setText(today);

        // 날짜 클릭 → DatePickerDialog
        editDate.setOnClickListener(v -> showDatePicker());

        // RecyclerView 세팅 (조회 전용 → 클릭 콜백 null)
        outerAdapter = new MedicineOuterAdapter(getContext(), listMedicine, null);
        rvOuter.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOuter.setAdapter(outerAdapter);

        // 조회 버튼
        btnSelect.setOnClickListener(v -> getIntakeList());

        // 가족 목록 로드
        loadFamilyList();

        return root;
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            calendar.setTime(sdf.parse(editDate.getText().toString()));
        } catch (Exception ignored) {}

        new DatePickerDialog(getActivity(), (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            editDate.setText(date);
        }, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    // 날짜 문자열 → 요일 계산 (0=월 ~ 6=일)
    private int getWeekdayFromDate(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK); // 1=일 ~ 7=토
            return (dayOfWeek + 5) % 7;                    // 0=월 ~ 6=일
        } catch (Exception e) {
            return 0;
        }
    }

    // 가족 목록 조회
    private void loadFamilyList() {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "relation_list")
                .add("no", userNo)
                .add("target_no", userNo)
                .add("date", editDate.getText().toString())
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
                    String responseData = response.body().string();
                    Log.i("HS", "가족목록 응답 : " + responseData);

                    JSONObject json     = new JSONObject(responseData);
                    JSONArray  jArrUser = json.getJSONArray("listTarget");

                    familyNames.clear();
                    familyNos.clear();

                    for (int i = 0; i < jArrUser.length(); i++) {
                        JSONObject obj = jArrUser.getJSONObject(i);
                        familyNames.add(obj.getString("name"));
                        familyNos.add(String.valueOf(obj.getLong("no")));
                    }

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (familyNames.isEmpty()) {
                            Toast.makeText(getContext(), "등록된 가족이 없습니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                getActivity(),ㅅ
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

    // 복약 내역 조회
    private void getIntakeList() {
        if (familyNos.isEmpty()) {
            Toast.makeText(getContext(), "가족을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        int    selectedIndex = spinnerTarget.getSelectedItemPosition();
        String targetNo      = familyNos.get(selectedIndex);
        String date          = editDate.getText().toString();
        int    weekday       = getWeekdayFromDate(date);

        Log.i("HS", "조회 targetNo=" + targetNo + " date=" + date + " weekday=" + weekday);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "get_family_intake_list") // ✅ 변경
                .add("target_no", targetNo)            // ✅ target_no로 변경
                .add("date", date)
                .add("weekday", String.valueOf(weekday))
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
                    String responseData = response.body().string();
                    Log.i("HS", "복약내역 응답 : " + responseData);

                    JSONArray jArr     = new JSONArray(responseData);
                    List<MedicineModel> tempList = new ArrayList<>();

                    for (int i = 0; i < jArr.length(); i++) {
                        JSONObject cardObj      = jArr.getJSONObject(i);
                        MedicineModel medicine  = new MedicineModel();
                        medicine.setName(cardObj.getString("medicine_name"));

                        JSONArray schedules = cardObj.getJSONArray("schedules");
                        List<MedicineModel> innerList = new ArrayList<>();

                        for (int j = 0; j < schedules.length(); j++) {
                            JSONObject    s     = schedules.getJSONObject(j);
                            MedicineModel inner = new MedicineModel();
                            inner.setNo(s.getString("schedule_no"));
                            inner.setType(s.getString("intake_time_type"));
                            inner.setTime(s.getString("intake_time"));
                            inner.setIntakeYn(String.valueOf(s.getInt("is_taken")));

                            Log.i("HS", "is_taken 원본=" + s.getInt("is_taken") + " intakeYn=" + inner.getIntakeYn()); // ✅
                            innerList.add(inner);
                        }

                        medicine.setListMedicine(innerList);
                        tempList.add(medicine);
                    }

                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
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