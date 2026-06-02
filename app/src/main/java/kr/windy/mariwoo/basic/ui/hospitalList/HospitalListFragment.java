package kr.windy.mariwoo.basic.ui.hospitalList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.adapter.HospitalListAdapter;
import kr.windy.mariwoo.basic.model.HospitalScheduleModel;

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

/**
 * 병원 스케줄 목록 Fragment
 * - 날짜 범위 선택 후 조회
 * - 등록/수정 → HospitalAddBottomSheet (모달)
 * - 삭제 → 확인 다이얼로그 후 서버 삭제
 */
public class HospitalListFragment extends Fragment {

    private EditText          editStartDate, editEndDate;
    private AppCompatButton   btnAdd;
    private RecyclerView      recyclerView;
    private LinearLayout      layoutEmpty;

    private HospitalListAdapter         adapter;
    private List<HospitalScheduleModel> list = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private String userNo    = "";
    private String serverUrl = "";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_hospital_list, container, false);

        serverUrl = getString(R.string.server_hospital);

        Activity activity = getActivity();
        if (activity == null) return root;
        sharedPreferences = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
        userNo = sharedPreferences.getString("user_no", "-1");

        editStartDate = root.findViewById(R.id.hospitalListFragment_editText_startDate);
        editEndDate   = root.findViewById(R.id.hospitalListFragment_editText_endDate);
        btnAdd        = root.findViewById(R.id.hospitalListFragment_button_add);
        recyclerView  = root.findViewById(R.id.hospitalListFragment_recyclerview);
        layoutEmpty   = root.findViewById(R.id.hospitalListFragment_layout_empty);

        // 기본 날짜: 오늘 기준 -30일 ~ +30일
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        editStartDate.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.DAY_OF_MONTH, 60);
        editEndDate.setText(sdf.format(cal.getTime()));

        // 날짜 선택 시 자동 조회
        editStartDate.setOnClickListener(v -> showDatePicker(editStartDate, true));
        editEndDate.setOnClickListener(v -> showDatePicker(editEndDate, false));

        adapter = new HospitalListAdapter(getContext(), list, new HospitalListAdapter.OnItemClickListener() {
            @Override
            public void onModify(HospitalScheduleModel item) {
                HospitalAddBottomSheet sheet = HospitalAddBottomSheet.newModifyInstance(
                        item.getNo(), item.getName(), item.getTime(), item.getMemo());
                sheet.setOnSavedListener(() -> getList());
                sheet.show(getChildFragmentManager(), "hospital_modify");
            }

            @Override
            public void onDelete(HospitalScheduleModel item) {
                showDeleteDialog(item);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            HospitalAddBottomSheet sheet = HospitalAddBottomSheet.newAddInstance();
            sheet.setOnSavedListener(() -> getList());
            sheet.show(getChildFragmentManager(), "hospital_add");
        });

        getList();
        return root;
    }

    private void showDatePicker(EditText target, boolean isStart) {
        Calendar cal = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            cal.setTime(sdf.parse(target.getText().toString()));
        } catch (Exception ignored) {}

        new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                    target.setText(date);
                    getList(); // 날짜 선택 완료 시 자동 조회
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showDeleteDialog(HospitalScheduleModel item) {
        Activity act = getActivity();
        if (act == null) return;
        new AlertDialog.Builder(act)
                .setMessage("스케줄을 삭제하시겠습니까?")
                .setPositiveButton("확인", (dialog, which) -> deleteItem(item))
                .setNegativeButton("취소", null)
                .show();
    }

    private void getList() {
        if (editStartDate == null || editEndDate == null) return;
        String startAt = editStartDate.getText().toString();
        String endAt   = editEndDate.getText().toString();

        RequestBody formBody = new FormBody.Builder()
                .add("cmd",      "list")
                .add("user_no",  userNo)
                .add("start_at", startAt)
                .add("end_at",   endAt)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Activity _fa = getActivity();
                if (_fa != null) _fa.runOnUiThread(() ->
                    android.widget.Toast.makeText(_fa, "네트워크 오류가 발생했습니다.", android.widget.Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;
                String responseData = response.body().string();
                Log.i("HS HospitalList", "응답: " + responseData);

                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        if (responseData.trim().startsWith("<")) {
                            Log.e("HS HospitalList", "서버 HTML 오류 응답: " + responseData);
                            Toast.makeText(act, "서버 오류: " + responseData.substring(0, Math.min(responseData.length(), 100)), Toast.LENGTH_LONG).show();
                            updateEmptyView();
                            return;
                        }

                        JSONObject json = new JSONObject(responseData);
                        JSONArray  jArr = json.optJSONArray("listSchedule");
                        if (jArr == null) { updateEmptyView(); return; }
                        list.clear();

                        for (int i = 0; i < jArr.length(); i++) {
                            JSONObject obj = jArr.getJSONObject(i);
                            HospitalScheduleModel item = new HospitalScheduleModel();
                            item.setNo(obj.getLong("no"));
                            item.setName(obj.getString("name"));
                            item.setTime(obj.getString("time"));
                            item.setMemo(obj.getString("memo"));
                            list.add(item);
                        }

                        if (adapter != null) adapter.notifyDataSetChanged();
                        updateEmptyView();
                    } catch (Exception e) {
                        Log.e("HS HospitalList", "파싱 오류: " + e.getMessage() + " / 응답: " + responseData);
                        e.printStackTrace();
                        updateEmptyView();
                    }
                });
            }
        });
    }

    private void deleteItem(HospitalScheduleModel item) {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "delete")
                .add("no",  String.valueOf(item.getNo()))
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Activity act = getActivity();
                if (act != null) act.runOnUiThread(() ->
                    Toast.makeText(act, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) return;
                String responseData = response.body().string();

                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if ("true".equals(json.getString("result"))) {
                            // 알람도 함께 취소
                            HospitalAlarmHelper.cancelAlarm(act, item.getNo());
                            getList();
                        } else {
                            Toast.makeText(act, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    private void updateEmptyView() {
        if (recyclerView == null || layoutEmpty == null) return;
        boolean isEmpty = list == null || list.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView  = null;
        layoutEmpty   = null;
        adapter       = null;
        editStartDate = null;
        editEndDate   = null;
        btnAdd        = null;
    }
}

