package kr.windy.mariwoo.basic.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 로그인 후 서버에서 약 알람 전체 복원
 *
 * 사용 흐름:
 *   로그인 성공 → restoreAlarms() 호출 → 서버에서 알람 목록 수신
 *   → AlarmHelper.setAlarm() 반복 등록 → onDone 콜백 실행 (MainActivity 이동 등)
 *
 * 서버 요청:
 *   cmd=get_all_alarms, no={user_no}
 *
 * 서버 응답 예시:
 *   {
 *     "result": "true",
 *     "list": [
 *       {
 *         "medicine_no":   "1",
 *         "medicine_name": "고혈압 약",
 *         "weekday":       0,
 *         "intake_time_type": "아침",
 *         "intake_time":   "07:00:00",
 *         "intake_type":   "식전"
 *       }, ...
 *     ]
 *   }
 */
public class AlarmRestoreHelper {

    private static final OkHttpClient client = new OkHttpClient();

    /**
     * 서버에서 해당 계정의 전체 알람을 가져와 AlarmManager에 재등록
     *
     * @param context    컨텍스트
     * @param userNo     사용자 번호 (SharedPreferences의 "user_no")
     * @param serverUrl  server_medicine URL
     * @param onDone     완료 후 실행할 콜백 — 성공/실패 모두 메인 스레드에서 호출됨
     */
    public static void restoreAlarms(Context context, String userNo,
                                     String serverUrl, Runnable onDone) {

        // 기존 SlotPreferences 초기화 (로그아웃 후 남은 찌꺼기 방지)
        AlarmHelper.cancelAllAlarmsForAllMedicines(context);

        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "get_all_alarms")
                .add("no",  userNo)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("AlarmRestoreHelper", "알람 복원 요청 실패: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(onDone);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() == null) {
                    Log.e("AlarmRestoreHelper", "응답 body가 null");
                    new Handler(Looper.getMainLooper()).post(onDone);
                    return;
                }
                final String responseData = response.body().string();

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        JSONObject json   = new JSONObject(responseData);
                        String     result = json.getString("result");

                        if ("true".equals(result)) {
                            JSONArray list = json.getJSONArray("list");

                            for (int i = 0; i < list.length(); i++) {
                                JSONObject item = list.getJSONObject(i);

                                String medicineNo   = item.getString("medicine_no");
                                String medicineName = item.getString("medicine_name");
                                int    weekday      = item.getInt("weekday");
                                String timeTypeStr  = item.getString("intake_time_type");
                                String intakeTime   = item.getString("intake_time"); // "07:00:00"
                                String intakeType   = item.getString("intake_type");

                                // "07:00:00" → "07:00" (AlarmHelper는 HH:mm 형식 사용)
                                if (intakeTime != null && intakeTime.length() >= 5) {
                                    intakeTime = intakeTime.substring(0, 5);
                                }

                                int timeType = convertTimeType(timeTypeStr);

                                AlarmHelper.setAlarm(
                                        context,
                                        medicineName,
                                        medicineNo,
                                        String.valueOf(weekday),
                                        timeType,
                                        intakeType,
                                        intakeTime
                                );
                            }

                            Log.i("AlarmRestoreHelper", "알람 복원 완료: " + list.length() + "개");

                        } else {
                            Log.i("AlarmRestoreHelper", "등록된 알람 없음 (result=false)");
                        }

                    } catch (Exception e) {
                        Log.e("AlarmRestoreHelper", "알람 복원 파싱 오류", e);
                    }

                    onDone.run(); // 성공/실패 모두 진행
                });
            }
        });
    }

    /**
     * 시간대 문자열 → AlarmHelper 시간대 인덱스 변환
     * "아침"(0) / "점심"(1) / "저녁"(2) / "취침 전"(3)
     */
    private static int convertTimeType(String timeTypeStr) {
        if (timeTypeStr == null) return 0;
        switch (timeTypeStr) {
            case "아침":    return 0;
            case "점심":    return 1;
            case "저녁":    return 2;
            case "취침 전": return 3;
            default:       return 0;
        }
    }
}
