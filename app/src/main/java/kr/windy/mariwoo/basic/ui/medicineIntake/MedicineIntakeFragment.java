package kr.windy.mariwoo.basic.ui.medicineIntake;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.adapter.MedicineCardAdapter;
import kr.windy.mariwoo.basic.model.MedicineCardItem;
import kr.windy.mariwoo.basic.model.MedicineScheduleItem;

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

/**
 * 복용 체크 Fragment (내 복용 현황)
 * - 날짜 선택 후 해당 날짜의 약 복용 스케줄 표시
 * - 각 항목 체크 → 서버에 복용 완료 기록
 * - 화면 진입 시 오늘 날짜로 자동 조회
 */
public class MedicineIntakeFragment extends Fragment {

    private TextView         tvDate;      // 날짜 표시 텍스트 (클릭 시 DatePicker 오픈)
    private AppCompatButton  btnSearch;   // 조회 버튼
    private RecyclerView     recyclerView;
    private MedicineCardAdapter cardAdapter;
    private List<MedicineCardItem> cardList = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private String userNo       = "";
    private String serverUrl    = "";
    private String selectedDate = ""; // 현재 선택된 날짜 (yyyy-MM-dd)

    // 싱글톤 OkHttpClient (loadIntakeList, checkIntake 공용)
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_medicine_intake, container, false);

        serverUrl = getString(R.string.server_medicine);
        sharedPreferences = requireActivity().getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        // View 초기화
        tvDate       = view.findViewById(R.id.medicineIntakeFragment_editText_date);
        btnSearch    = view.findViewById(R.id.medicineIntakeFragment_button_add);
        recyclerView = view.findViewById(R.id.medicineIntakeFragment_recyclerview);

        // 오늘 날짜로 초기화 (내부 저장용: yyyy-MM-dd, 표시용: yyyy년 MM월 dd일)
        selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvDate.setText(new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault()).format(new Date()));

        // 날짜 텍스트 클릭 → DatePickerDialog
        tvDate.setOnClickListener(v -> showDatePicker());

        // RecyclerView 설정 (체크 클릭 → checkIntake 호출)
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        cardAdapter = new MedicineCardAdapter(cardList, (item, position) ->
                checkIntake(item, position) // 복용 체크 서버 전송
        );
        recyclerView.setAdapter(cardAdapter);

        // 조회 버튼
        btnSearch.setOnClickListener(v -> loadIntakeList());

        // 화면 진입 시 오늘 날짜 자동 조회
        loadIntakeList();

        return view;
    }

    /**
     * 날짜 선택 다이얼로그
     * 선택한 날짜로 selectedDate 및 tvDate 업데이트
     */
    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    // 내부 저장: yyyy-MM-dd (서버 전송용)
                    selectedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(selected.getTime());
                    // 화면 표시: "2025년 5월 13일"
                    tvDate.setText(year + "년 " + (month + 1) + "월 " + dayOfMonth + "일");
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    /**
     * 복용 내역 조회
     * selectedDate의 요일을 계산해 함께 전송
     * 서버는 요일로 스케줄을 필터링
     */
    private void loadIntakeList() {
        int weekday = getWeekday(selectedDate); // 날짜 → 요일 변환

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",     "get_intake_list")
                .add("user_no", userNo)
                .add("date",    selectedDate)
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
                try {
                    if (response.body() == null) return;
                    String    responseData = response.body().string();
                    Log.i("HS", "intake list 응답: " + responseData);

                    // 응답: JSONArray 형태 [{medicine_name, schedules:[...]}, ...]
                    JSONArray              jsonArray = new JSONArray(responseData);
                    List<MedicineCardItem> newList   = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject    cardJson = jsonArray.getJSONObject(i);
                        MedicineCardItem card  = new MedicineCardItem();
                        card.setMedicineName(cardJson.getString("medicine_name"));

                        JSONArray                 scheduleArray = cardJson.getJSONArray("schedules");
                        List<MedicineScheduleItem> schedules    = new ArrayList<>();

                        for (int j = 0; j < scheduleArray.length(); j++) {
                            JSONObject         scheduleJson = scheduleArray.getJSONObject(j);
                            MedicineScheduleItem schedule   = new MedicineScheduleItem();
                            schedule.setScheduleNo(scheduleJson.getLong("schedule_no"));
                            schedule.setIntakeTimeType(scheduleJson.getString("intake_time_type")); // 아침/점심 등
                            schedule.setIntakeType(scheduleJson.getString("intake_type"));         // 식전/식후
                            schedule.setIntakeTime(scheduleJson.getString("intake_time"));         // HH:mm
                            schedule.setTaken(scheduleJson.getInt("is_taken") == 1);              // 복용 여부
                            schedules.add(schedule);
                        }
                        card.setSchedules(schedules);
                        newList.add(card);
                    }

                    // UI 업데이트 (메인 스레드)
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

    /**
     * 복용 체크 저장
     * 체크한 항목을 서버에 기록하고 UI 즉시 업데이트
     *
     * @param item     체크한 스케줄 항목
     * @param position RecyclerView 내 위치 (현재 미사용, 확장성 고려)
     */
    private void checkIntake(MedicineScheduleItem item, int position) {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd",         "check_intake")
                .add("schedule_no", String.valueOf(item.getScheduleNo()))
                .add("date",        selectedDate)
                .add("is_taken",    "1") // 복용 완료
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
                    String     responseData = response.body().string();
                    JSONObject json         = new JSONObject(responseData);

                    if ("true".equals(json.getString("result"))) {
                        // 서버 저장 성공 → UI 즉시 체크 표시
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 뷰 참조 해제 (메모리 누수 방지)
        tvDate       = null;
        btnSearch    = null;
        recyclerView = null;
        cardAdapter  = null;
    }

    /**
     * 날짜 문자열 → 요일 인덱스 변환
     * Calendar.DAY_OF_WEEK: 1=일 ~ 7=토
     * 반환값: 0=월 ~ 6=일
     */
    private int getWeekday(String date) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(date));
            int day = cal.get(Calendar.DAY_OF_WEEK); // 1:일 ~ 7:토
            return (day == 1) ? 6 : day - 2;         // 0:월 ~ 6:일 로 변환
        } catch (Exception e) {
            e.printStackTrace();
            return 0; // 파싱 실패 시 월요일 기본값
        }
    }
}
