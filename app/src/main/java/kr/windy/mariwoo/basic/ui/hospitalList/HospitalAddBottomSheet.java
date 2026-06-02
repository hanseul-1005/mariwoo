package kr.windy.mariwoo.basic.ui.hospitalList;

import android.app.Activity;
import android.app.DatePickerDialog;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.helper.HospitalAlarmHelper;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 병원 스케줄 등록/수정 BottomSheet
 * - mode "add"    : 등록
 * - mode "modify" : no, name, time, memo 전달 받아 수정
 * 완료 시 onSaved 콜백으로 목록 새로고침
 */
public class HospitalAddBottomSheet extends BottomSheetDialogFragment {

    public interface OnSavedListener {
        void onSaved();
    }

    private static final String ARG_MODE = "mode";
    private static final String ARG_NO   = "no";
    private static final String ARG_NAME = "name";
    private static final String ARG_TIME = "time";
    private static final String ARG_MEMO = "memo";

    private EditText        editName, editDate, editTime, editMemo;
    private AppCompatButton btnSave;

    private String serverUrl = "";
    private String userNo    = "";
    private String mode      = "add";
    private long   scheduleNo = -1;

    private OnSavedListener savedListener;

    private final OkHttpClient client = new OkHttpClient();

    // 등록 모드
    public static HospitalAddBottomSheet newAddInstance() {
        HospitalAddBottomSheet sheet = new HospitalAddBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, "add");
        sheet.setArguments(args);
        return sheet;
    }

    // 수정 모드
    public static HospitalAddBottomSheet newModifyInstance(long no, String name, String time, String memo) {
        HospitalAddBottomSheet sheet = new HospitalAddBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MODE, "modify");
        args.putLong(ARG_NO,     no);
        args.putString(ARG_NAME, name);
        args.putString(ARG_TIME, time);
        args.putString(ARG_MEMO, memo);
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnSavedListener(OnSavedListener listener) {
        this.savedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_hospital_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        serverUrl = getString(R.string.server_hospital);
        Activity activity = getActivity();
        if (activity != null) {
            SharedPreferences sp = activity.getSharedPreferences("autoLogin", Activity.MODE_PRIVATE);
            userNo = sp.getString("user_no", "-1");
        }

        editName = view.findViewById(R.id.hospitalAddActivity_editText_name);
        editDate = view.findViewById(R.id.hospitalAddActivity_editText_date);
        editTime = view.findViewById(R.id.hospitalAddActivity_editText_time);
        editMemo = view.findViewById(R.id.hospitalAddActivity_editText_memo);
        btnSave  = view.findViewById(R.id.hospitalAddActivity_button_save);

        Bundle args = getArguments();
        if (args != null) {
            mode = args.getString(ARG_MODE, "add");
        }

        if ("modify".equals(mode) && args != null) {
            scheduleNo = args.getLong(ARG_NO, -1);
            String time = args.getString(ARG_TIME, "");

            editName.setText(args.getString(ARG_NAME, ""));
            editMemo.setText(args.getString(ARG_MEMO, ""));
            btnSave.setText("병원 스케줄 수정");

            if (time.contains(" ")) {
                String[] parts = time.split(" ");
                editDate.setText(parts[0]);
                editTime.setText(parts[1]);
            } else {
                editDate.setText(time);
            }
        } else {
            // 등록 기본값: 오늘 날짜, 현재 시간
            editDate.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Calendar.getInstance().getTime()));
            Calendar cal = Calendar.getInstance();
            editTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
        }

        // 날짜 클릭 → DatePickerDialog
        editDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            try {
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .parse(editDate.getText().toString()));
            } catch (Exception ignored) {}
            new DatePickerDialog(requireContext(), (picker, year, month, day) ->
                    editDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 시간 클릭 → MaterialTimePicker (키보드 입력 모드)
        editTime.setOnClickListener(v -> {
            int hour = 9, minute = 0;
            try {
                String[] t = editTime.getText().toString().split(":");
                hour   = Integer.parseInt(t[0]);
                minute = Integer.parseInt(t[1]);
            } catch (Exception ignored) {}

            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setInputMode(MaterialTimePicker.INPUT_MODE_KEYBOARD)
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(hour)
                    .setMinute(minute)
                    .setTitleText("시간 선택")
                    .build();

            picker.addOnPositiveButtonClickListener(btn ->
                    editTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                            picker.getHour(), picker.getMinute())));

            picker.show(getChildFragmentManager(), "time_picker");
        });

        btnSave.setOnClickListener(v -> save());
    }

    private void save() {
        String name = editName.getText().toString().trim();
        String date = editDate.getText().toString();
        String time = editTime.getText().toString();
        String memo = editMemo.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "병원명을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullTime = date + " " + time;

        FormBody.Builder builder = new FormBody.Builder()
                .add("name",              name)
                .add("time",              fullTime)
                .add("notification_time", fullTime)
                .add("memo",              memo);

        if ("modify".equals(mode)) {
            builder.add("cmd", "modify");
            builder.add("no",  String.valueOf(scheduleNo));
        } else {
            builder.add("cmd",     "add");
            builder.add("user_no", userNo);
        }

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(builder.build())
                .build();

        Log.i("HS HospitalAdd", "mode=" + mode);

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

                Activity act = getActivity();
                if (act == null || act.isFinishing()) return;

                act.runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if ("true".equals(json.getString("result"))) {
                            String msg = "modify".equals(mode) ? "수정되었습니다." : "등록되었습니다.";
                            Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();

                            // scheduleNo 결정
                            // 수정: 기존 no 사용
                            // 등록: 서버 응답의 no 사용, 없으면 fullTime 해시로 대체
                            long alarmNo;
                            if ("modify".equals(mode)) {
                                alarmNo = scheduleNo;
                            } else {
                                alarmNo = json.optLong("no", -1);
                                if (alarmNo <= 0) {
                                    // 서버가 no를 안 돌려줄 경우 fullTime 해시로 대체
                                    alarmNo = (long)(fullTime.hashCode() & 0x7FFFFFFF);
                                    Log.w("HospitalAdd", "서버에서 no 미반환 → fullTime 해시 사용: " + alarmNo);
                                }
                            }

                            // 수정 시 기존 알람 취소 후 재등록
                            if ("modify".equals(mode)) {
                                HospitalAlarmHelper.cancelAlarm(act, alarmNo);
                            }
                            HospitalAlarmHelper.setAlarm(act, alarmNo, name, fullTime);

                            if (savedListener != null) savedListener.onSaved();
                            if (isAdded()) dismiss();
                        } else {
                            Toast.makeText(act, "다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }
}

