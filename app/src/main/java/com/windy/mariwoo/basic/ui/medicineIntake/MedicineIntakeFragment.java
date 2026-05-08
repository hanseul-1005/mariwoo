package com.windy.mariwoo.basic.ui.medicineIntake;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.activity.DatePickerActivity;
import com.windy.mariwoo.basic.activity.MedicineAddActivity;
import com.windy.mariwoo.basic.adapter.MedicineCardAdapter;
import com.windy.mariwoo.basic.adapter.MedicineOuterAdapter;
import com.windy.mariwoo.basic.model.MedicineCardItem;
import com.windy.mariwoo.basic.model.MedicineModel;
import com.windy.mariwoo.basic.model.MedicineScheduleItem;
import com.windy.mariwoo.databinding.FragmentMedicineIntakeBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MedicineIntakeFragment extends Fragment {

    private TextView tvDate;
    private AppCompatButton btnSearch;
    private RecyclerView recyclerView;
    private MedicineCardAdapter cardAdapter;
    private List<MedicineCardItem> cardList = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";
    private String selectedDate = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medicine_intake, container, false);

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = requireActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        tvDate    = view.findViewById(R.id.medicineIntakeFragment_editText_date);
        btnSearch = view.findViewById(R.id.medicineIntakeFragment_button_add);
        recyclerView = view.findViewById(R.id.medicineIntakeFragment_recyclerview);

        // 오늘 날짜 기본값
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText(new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(new Date()));

        // 날짜 클릭 → DatePickerDialog
        tvDate.setOnClickListener(v -> showDatePicker());

        // RecyclerView 설정
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new MedicineCardAdapter(cardList, (item, position) -> {
            // 먹음 체크 서버 전송
            checkIntake(item, position);
        });
        recyclerView.setAdapter(cardAdapter);

        // 조회 버튼
        btnSearch.setOnClickListener(v -> loadIntakeList());

        // 화면 진입 시 자동 조회
        loadIntakeList();

        return view;
    }

    // 날짜 선택
    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selected.getTime());
                    tvDate.setText(year + "년 " + (month + 1) + "월 " + dayOfMonth + "일");
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // 복용 내역 조회
    private void loadIntakeList() {
        // 선택한 날짜의 요일 계산 (0:월 ~ 6:일)
        int weekday = getWeekday(selectedDate);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "get_intake_list")
                .add("user_no", userNo)
                .add("date", selectedDate)
                .add("weekday", String.valueOf(weekday))
                .build();

        OkHttpClient client = new OkHttpClient();
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
                try {
                    if (response.body() == null) return;
                    String responseData = response.body().string();
                    Log.i("HS", "intake list 응답: " + responseData);

                    JSONArray jsonArray = new JSONArray(responseData);
                    List<MedicineCardItem> newList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject cardJson = jsonArray.getJSONObject(i);

                        MedicineCardItem card = new MedicineCardItem();
                        card.setMedicineName(cardJson.getString("medicine_name"));

                        JSONArray scheduleArray = cardJson.getJSONArray("schedules");
                        List<MedicineScheduleItem> schedules = new ArrayList<>();

                        for (int j = 0; j < scheduleArray.length(); j++) {
                            JSONObject scheduleJson = scheduleArray.getJSONObject(j);

                            MedicineScheduleItem schedule = new MedicineScheduleItem();
                            schedule.setScheduleNo(scheduleJson.getLong("schedule_no"));
                            schedule.setIntakeTimeType(scheduleJson.getString("intake_time_type"));
                            schedule.setIntakeType(scheduleJson.getString("intake_type"));
                            schedule.setIntakeTime(scheduleJson.getString("intake_time"));
                            schedule.setTaken(scheduleJson.getInt("is_taken") == 1);

                            schedules.add(schedule);
                        }
                        card.setSchedules(schedules);
                        newList.add(card);
                    }

                    Activity act = getActivity();
                    if (act == null || act.isFinishing()) return;
                    act.runOnUiThread(() -> {
                        cardList.clear();
                        cardList.addAll(newList);
                        cardAdapter.notifyDataSetChanged();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 복용 체크
    private void checkIntake(MedicineScheduleItem item, int position) {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "check_intake")
                .add("schedule_no", String.valueOf(item.getScheduleNo()))
                .add("date", selectedDate)
                .add("is_taken", "1")
                .build();

        OkHttpClient client = new OkHttpClient();
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
                try {
                    if (response.body() == null) return;
                    String responseData = response.body().string();
                    JSONObject json = new JSONObject(responseData);

                    if ("true".equals(json.getString("result"))) {
                        Activity act = getActivity();
                        if (act == null || act.isFinishing()) return;
                        act.runOnUiThread(() -> {
                            item.setTaken(true);
                            cardAdapter.notifyDataSetChanged();
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 날짜 → 요일 변환 (0:월 ~ 6:일)
    private int getWeekday(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            int day = cal.get(Calendar.DAY_OF_WEEK); // 1:일 ~ 7:토
            return (day == 1) ? 6 : day - 2; // 0:월 ~ 6:일 로 변환
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}