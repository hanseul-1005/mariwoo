// MedicineCalendarFragment.java
package com.windy.mariwoo.basic.ui.medicineCalendar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.adapter.CalendarDayAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MedicineCalendarFragment extends Fragment {

    private TextView tvYearMonth, tvMonth; // ✅ tvMonth 추가
    private AppCompatButton btnSearch;
    private RecyclerView recyclerView;
    private CalendarDayAdapter calendarDayAdapter;

    private Calendar currentCalendar = Calendar.getInstance();

    private SharedPreferences sharedPreferences;
    private String userNo = "";
    private String serverUrl = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medicine_calendar, container, false);

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = requireActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        tvYearMonth = view.findViewById(R.id.calendarFragment_text_yearMonth);
        tvMonth = view.findViewById(R.id.calendarFragment_text_month);
        btnSearch   = view.findViewById(R.id.calendarFragment_button_search);

        // < > 버튼
        view.findViewById(R.id.calendarFragment_button_prev).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            loadCalendar();
        });
        view.findViewById(R.id.calendarFragment_button_next).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            loadCalendar();
        });

        // 조회 버튼
        btnSearch.setOnClickListener(v -> loadCalendar());

        // RecyclerView - 7칸 그리드
        recyclerView = view.findViewById(R.id.calendarFragment_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarDayAdapter = new CalendarDayAdapter(new ArrayList<>(), new HashMap<>());
        recyclerView.setAdapter(calendarDayAdapter);

        // 초기 로드
        loadCalendar();

        return view;
    }

    private void loadCalendar() {
        int year  = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1;


        // 헤더 텍스트 업데이트
        Activity act = getActivity();
        if (act != null && !act.isFinishing()) {
            act.runOnUiThread(() -> {
                tvYearMonth.setText(year + "년 " + month + "월");
                tvMonth.setText(month + "월");
            });
        }

        // 달력 날짜 리스트 생성
        List<Integer> dayList = buildDayList(year, month);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "get_calendar_intake")
                .add("user_no", userNo)
                .add("year", String.valueOf(year))
                .add("month", String.valueOf(month))
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
                    Log.i("HS", "calendar 응답: " + responseData);
                    JSONArray jsonArray = new JSONArray(responseData);

                    // key: "2025-02-13", value: "all" or "partial"
                    // all    → 초록색 (전부 복용)
                    // partial → 빨간색 (일부 미복용)
                    Map<String, String> statusMap = new HashMap<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String date   = obj.getString("date");
                        String status = obj.getString("status"); // "all" or "partial"
                        statusMap.put(date, status);
                    }

                    Activity uiAct = getActivity();
                    if (uiAct == null || uiAct.isFinishing()) return;
                    uiAct.runOnUiThread(() -> {
                        calendarDayAdapter.updateData(dayList, statusMap,
                                year, month);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 해당 월의 날짜 리스트 생성 (앞뒤 빈칸 포함)
    // 0이면 빈칸, 양수면 날짜
    private List<Integer> buildDayList(int year, int month) {
        List<Integer> list = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);

        // 1일의 요일 (일=1, 월=2 ... 토=7)
        // 일요일부터 시작이므로 (dayOfWeek - 1)칸 빈칸
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0:일 ~ 6:토

        // 빈칸 추가
        for (int i = 0; i < firstDayOfWeek; i++) {
            list.add(0);
        }

        // 날짜 추가
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= maxDay; d++) {
            list.add(d);
        }

        // 마지막 주 빈칸 채우기 (7의 배수로)
        while (list.size() % 7 != 0) {
            list.add(0);
        }

        return list;
    }
}