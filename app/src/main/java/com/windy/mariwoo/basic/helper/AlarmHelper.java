package com.windy.mariwoo.basic.helper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.windy.mariwoo.basic.receiver.AlarmReceiver;

import java.util.Calendar;

/**
 * 약 복용 알람 등록/취소 헬퍼 클래스
 *
 * 알람 구조:
 *   - 요일(0:월 ~ 6:일) × 시간대(0:아침 ~ 3:취침 전) 조합으로 개별 알람 등록
 *   - requestCode = (medicineNo + dayIndex + timeType).hashCode()
 *     → 약 번호 기반이므로 같은 약의 같은 요일/시간대 알람은 항상 동일한 코드
 *     → 수정 시 기존 알람 덮어쓰기 / 취소 시 정확한 타겟 지정 가능
 *
 * 알람 발동 후 자동 재등록:
 *   - AlarmReceiver.onReceive() → AlarmHelper.setAlarm() 호출
 *   - 다음 주 동일 요일/시간으로 재등록 (주간 반복)
 */
public class AlarmHelper {

    /**
     * 알람 등록
     *
     * @param context      컨텍스트
     * @param medicineName 약 이름 (알림 표시용)
     * @param medicineNo   약 번호 (requestCode 생성 기준)
     * @param weekday      요일 문자열 (쉼표 구분 가능, 예: "0,2,4" or "1")
     * @param timeType     시간대 인덱스 (0:아침, 1:점심, 2:저녁, 3:취침 전)
     * @param intakeType   식전/식후
     * @param time         복용 시간 (HH:mm)
     */
    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarm(Context context, String medicineName, String medicineNo,
                                String weekday, int timeType, String intakeType, String time) {

        if (time == null || time.isEmpty()) return;

        // 시간 파싱 (HH:mm → hour, minute)
        String[] timeParts = time.split(":");
        int hour   = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // 요일 파싱: "0,2,4" → ["0", "2", "4"]
        String[] days = weekday.split(",");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 시간대 인덱스 → 한글 이름 변환 (알림 텍스트에 사용)
        String timeTypeName = getTimeTypeName(timeType);

        for (String day : days) {
            int dayIndex    = Integer.parseInt(day.trim());
            int calendarDay = convertToCalendarDay(dayIndex); // Calendar 상수로 변환

            // 해당 요일/시간으로 Calendar 설정
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // 이미 지난 시간이면 다음 주 동일 요일로 설정
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            // 고유한 requestCode 생성: 약 번호 + 요일 + 시간대 조합
            // → 같은 약의 같은 요일/시간대는 항상 동일한 코드 (PendingIntent 덮어쓰기/취소 가능)
            int requestCode = Math.abs((medicineNo + dayIndex + timeType).hashCode());

            // AlarmReceiver에 전달할 Intent (알람 발동 시 AlarmReceiver.onReceive() 호출)
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("medicine_name",  medicineName);
            intent.putExtra("medicine_no",    medicineNo);    // 다음 주 알람 재등록에 사용
            intent.putExtra("intake_type",    intakeType);    // 식전/식후
            intent.putExtra("time_type",      timeTypeName);  // 아침/점심/저녁/취침 전
            intent.putExtra("weekday",        String.valueOf(dayIndex)); // 재등록용 요일
            intent.putExtra("time",           time);          // 재등록용 시간
            intent.putExtra("time_type_index", timeType);     // 재등록용 시간대 인덱스

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Doze 모드에서도 정확한 시간에 알람 발동 (API 23+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d("AlarmHelper", "알람 설정됨: " + medicineName + " / " + calendar.getTime().toString());
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * 특정 요일/시간대 알람 취소
     * setAlarm()과 동일한 requestCode로 PendingIntent를 찾아 취소
     *
     * @param medicineNo 약 번호
     * @param weekday    요일 (단일 값, 예: "1")
     * @param timeType   시간대 인덱스 (0~3)
     */
    public static void cancelAlarm(Context context, String medicineNo, String weekday, int timeType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int dayIndex    = Integer.parseInt(weekday.trim());

        // setAlarm()과 동일한 방식으로 requestCode 생성
        int requestCode = Math.abs((medicineNo + dayIndex + timeType).hashCode());

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent); // AlarmManager에서 알람 취소
        pendingIntent.cancel();             // PendingIntent도 취소

        Log.d("AlarmHelper", "알람 취소됨: medicineNo=" + medicineNo
                + " / 요일:" + dayIndex + " / 시간타입:" + timeType);
    }

    /**
     * 특정 약의 모든 요일/시간대 알람 취소
     * 약 삭제 시 호출 (0~6 요일 × 0~3 시간대 = 최대 28개 알람 취소)
     *
     * @param medicineNo 약 번호
     */
    public static void cancelAllAlarms(Context context, String medicineNo) {
        for (int day = 0; day <= 6; day++) {
            for (int timeType = 0; timeType <= 3; timeType++) {
                cancelAlarm(context, medicineNo, String.valueOf(day), timeType);
            }
        }
        Log.d("AlarmHelper", "전체 알람 취소됨: medicineNo=" + medicineNo);
    }

    /**
     * 앱 내부 요일 인덱스 → Calendar 요일 상수 변환
     * 앱: 0=월 ~ 6=일
     * Calendar: MONDAY=2, TUESDAY=3, ..., SUNDAY=1
     */
    private static int convertToCalendarDay(int dayIndex) {
        switch (dayIndex) {
            case 0: return Calendar.MONDAY;
            case 1: return Calendar.TUESDAY;
            case 2: return Calendar.WEDNESDAY;
            case 3: return Calendar.THURSDAY;
            case 4: return Calendar.FRIDAY;
            case 5: return Calendar.SATURDAY;
            case 6: return Calendar.SUNDAY;
            default: return Calendar.MONDAY;
        }
    }

    /**
     * 시간대 인덱스 → 한글 이름 변환 (알림 메시지 표시용)
     */
    private static String getTimeTypeName(int timeType) {
        switch (timeType) {
            case 0: return "아침";
            case 1: return "점심";
            case 2: return "저녁";
            case 3: return "취침 전";
            default: return "";
        }
    }
}
