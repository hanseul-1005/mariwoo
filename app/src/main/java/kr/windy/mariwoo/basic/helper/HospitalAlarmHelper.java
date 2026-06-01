package kr.windy.mariwoo.basic.helper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import kr.windy.mariwoo.basic.receiver.HospitalAlarmReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 병원 스케줄 단일 알람 헬퍼
 *
 * reqCode 규칙:
 *   1시간 전  : "hospital_{no}".hashCode()     & 0x7FFFFFFF
 *   24시간 전 : "hospital_day_{no}".hashCode() & 0x7FFFFFFF
 */
public class HospitalAlarmHelper {

    private static int reqCode1h(long no)  { return ("hospital_"     + no).hashCode() & 0x7FFFFFFF; }
    private static int reqCodeDay(long no) { return ("hospital_day_" + no).hashCode() & 0x7FFFFFFF; }

    /**
     * 알람 등록 (방문 24시간 전 + 1시간 전)
     *
     * @param scheduleNo hospital_schedule.no
     * @param name       병원명
     * @param fullTime   방문 일시 "yyyy-MM-dd HH:mm"
     */
    public static void setAlarm(Context context, long scheduleNo, String name, String fullTime) {
        scheduleOne(context, reqCodeDay(scheduleNo), name, fullTime, 24, "24시간");
        scheduleOne(context, reqCode1h(scheduleNo),  name, fullTime,  1, "1시간");
    }

    /**
     * 알람 취소 (24시간 전 + 1시간 전 둘 다)
     *
     * @param scheduleNo hospital_schedule.no
     */
    public static void cancelAlarm(Context context, long scheduleNo) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        cancelOne(context, am, reqCodeDay(scheduleNo));
        cancelOne(context, am, reqCode1h(scheduleNo));
        Log.d("HospitalAlarmHelper", "병원 알람 취소: no=" + scheduleNo);
    }

    @SuppressLint("ScheduleExactAlarm")
    private static void scheduleOne(Context context, int reqCode, String name,
                                    String fullTime, int hoursBefore, String label) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // Android 12+ 정확한 알람 권한 확인
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!am.canScheduleExactAlarms()) {
                    Log.w("HospitalAlarmHelper", "정확한 알람 권한 없음 → 일반 알람으로 대체");
                    // 권한 없어도 set()으로 대체 등록 (덜 정확하지만 울림)
                    scheduleInexact(context, am, reqCode, name, fullTime, hoursBefore, label);
                    return;
                }
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date visitDate = sdf.parse(fullTime);
            if (visitDate == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(visitDate);
            cal.add(Calendar.HOUR_OF_DAY, -hoursBefore);
            long triggerMillis = cal.getTimeInMillis();

            if (triggerMillis <= System.currentTimeMillis()) {
                Log.w("HospitalAlarmHelper", label + " 알람 시각이 이미 지났습니다: " + fullTime);
                return;
            }

            Intent intent = new Intent(context, HospitalAlarmReceiver.class);
            intent.putExtra("hospital_name",  name);
            intent.putExtra("hospital_time",  fullTime);
            intent.putExtra("hospital_label", label);
            intent.putExtra("request_code",   reqCode);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
            }

            Log.d("HospitalAlarmHelper", "알람 등록 [" + label + "]: " + name + " → " + sdf.format(cal.getTime()));

        } catch (Exception e) {
            Log.e("HospitalAlarmHelper", label + " 알람 등록 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void scheduleInexact(Context context, AlarmManager am, int reqCode,
                                        String name, String fullTime,
                                        int hoursBefore, String label) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date visitDate = sdf.parse(fullTime);
            if (visitDate == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(visitDate);
            cal.add(Calendar.HOUR_OF_DAY, -hoursBefore);
            long triggerMillis = cal.getTimeInMillis();

            if (triggerMillis <= System.currentTimeMillis()) return;

            Intent intent = new Intent(context, HospitalAlarmReceiver.class);
            intent.putExtra("hospital_name",  name);
            intent.putExtra("hospital_time",  fullTime);
            intent.putExtra("hospital_label", label);
            intent.putExtra("request_code",   reqCode);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, reqCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi);
            Log.d("HospitalAlarmHelper", "일반 알람 등록 [" + label + "]: " + sdf.format(cal.getTime()));
        } catch (Exception e) {
            Log.e("HospitalAlarmHelper", "일반 알람 등록 실패: " + e.getMessage());
        }
    }

    private static void cancelOne(Context context, AlarmManager am, int reqCode) {
        Intent intent = new Intent(context, HospitalAlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
