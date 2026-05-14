// MedicineCalendarFragment.java
package kr.windy.mariwoo.basic.ui.medicineCalendar;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.adapter.CalendarDayAdapter;

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

/**
 * 약 복용 달력 Fragment
 * - 월별 복용 현황을 달력 형태로 표시
 * - 날짜별 색상:
 *     초록색(all)   - 해당 날짜 모든 약 복용 완료
 *     빨간색(partial) - 일부 미복용
 *     무색           - 스케줄 없거나 미래 날짜
 * - < > 버튼으로 이전/다음 달 이동
 */
public class MedicineCalendarFragment extends Fragment {

    private TextView        tvYearMonth; // 상단 "2025년 5월" 텍스트
    private TextView        tvMonth;     // 달력 내부 월 표시 텍스트
    private AppCompatButton btnSearch;   // 조회 버튼
    private RecyclerView    recyclerView;
    private CalendarDayAdapter calendarDayAdapter;

    // 현재 표시 중인 연/월 (< > 버튼으로 변경)
    private Calendar currentCalendar = Calendar.getInstance();

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";

    // 싱글톤 OkHttpClient
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medicine_calendar, container, false);

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = requireActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // View 초기화
        tvYearMonth = view.findViewById(R.id.calendarFragment_text_yearMonth);
        tvMonth     = view.findViewById(R.id.calendarFragment_text_month);
        btnSearch   = view.findViewById(R.id.calendarFragment_button_search);

        // 이전 달 버튼
        view.findViewById(R.id.calendarFragment_button_prev).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            loadCalendar();
        });

        // 다음 달 버튼
        view.findViewById(R.id.calendarFragment_button_next).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            loadCalendar();
        });

        // 조회 버튼 (현재 달 재조회)
        btnSearch.setOnClickListener(v -> loadCalendar());

        // RecyclerView - 7칸 그리드 (일~토 7일 기준)
        recyclerView = view.findViewById(R.id.calendarFragment_recyclerview);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 7));

        calendarDayAdapter = new CalendarDayAdapter(new ArrayList<>(), new HashMap<>());
        recyclerView.setAdapter(calendarDayAdapter);

        // 초기 달력 로드 (현재 월)
        loadCalendar();

        return view;
    }

    /**
     * 서버에서 해당 월의 복용 현황 조회 후 달력 갱신
     * - 헤더 텍스트 업데이트
     * - 날짜 리스트 생성 (buildDayList)
     * - 서버 응답으로 statusMap 구성 (날짜 → "all"/"partial")
     */
    private void loadCalendar() {
        int year  = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1; // Calendar.MONTH는 0부터 시작

        // 헤더 텍스트 업데이트 (메인 스레드 안전 처리)
        Activity act = getActivity();
        if (act != null && !act.isFinishing()) {
            act.runOnUiThread(() -> {
                tvYearMonth.setText(year + "년 " + month + "월");
                tvMonth.setText(month + "월");
            });
        }

        // 달력 날짜 리스트 (빈칸 포함, buildDayList 참고)
        List<Integer> dayList = buildDayList(year, month);

        // 서버에 해당 월 복용 현황 요청
        RequestBody formBody = new FormBody.Builder()
                .add("cmd",     "get_calendar_intake")
                .add("user_no", userNo)
                .add("year",    String.valueOf(year))
                .add("month",   String.valueOf(month))
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
                try {
                    if (response.body() == null) return;
                    String    responseData = response.body().string();
                    Log.i("HS", "calendar 응답: " + responseData);

                    JSONArray jsonArray = new JSONArray(responseData);

                    // 날짜 → 복용 상태 매핑
                    // key: "2025-05-13", value: "all" or "partial"
                    Map<String, String> statusMap = new HashMap<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj    = jsonArray.getJSONObject(i);
                        String     date   = obj.getString("date");
                        String     status = obj.getString("status"); // "all" or "partial"
                        statusMap.put(date, status);
                    }

                    // 달력 어댑터 데이터 갱신 (메인 스레드)
                    Activity uiAct = getActivity();
                    if (uiAct == null || uiAct.isFinishing()) return;
                    uiAct.runOnUiThread(() ->
                            calendarDayAdapter.updateData(dayList, statusMap, year, month));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * 해당 월의 달력 날짜 리스트 생성
     * - 1일 전 빈칸(0), 실제 날짜, 마지막 주 빈칸(0) 포함
     * - 7칸 그리드에 맞게 0으로 패딩
     *
     * 예: 2025년 5월 1일이 목요일이면
     *   [0, 0, 0, 0, 1, 2, 3, 4, 5, ..., 31, 0, 0, ...]
     *
     * @return 날짜 리스트 (0=빈칸, 양수=날짜)
     */
    private List<Integer> buildDayList(int year, int month) {
        List<Integer> list = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1); // month-1: Calendar는 0부터 시작

        // 1일의 요일 (일=1, 월=2, ... 토=7) → 앞 빈칸 개수
        // Calendar.DAY_OF_WEEK - 1 → 0(일) ~ 6(토)
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        // 1일 전 빈칸 추가
        for (int i = 0; i < firstDayOfWeek; i++) {
            list.add(0);
        }

        // 날짜 추가 (1일 ~ 말일)
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= maxDay; d++) {
            list.add(d);
        }

        // 마지막 주 빈칸 채우기 (7의 배수가 되도록)
        while (list.size() % 7 != 0) {
            list.add(0);
        }

        return list;
    }
}
