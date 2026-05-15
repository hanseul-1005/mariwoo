package kr.windy.mariwoo.basic.helper;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import kr.windy.mariwoo.basic.receiver.AlarmReceiver;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 약 복용 알람 등록/취소 헬퍼 클래스
 *
 * 알람 구조:
 *   - 시간 슬롯(요일 × 시간대 × 시각) 기반으로 알람을 하나만 등록
 *   - 같은 시간에 여러 약이 있어도 AlarmManager 알람은 1개 (덮어쓰기 방지)
 *   - SharedPreferences("alarm_slots")에 슬롯별 약 목록을 저장
 *     key: "slot_{dayIndex}_{timeType}_{HHmm}"  ex) "slot_0_0_0800"
 *     value: Set<String> = {"medicineNo||medicineName||intakeType", ...}
 *
 * 알람 발동 후 자동 재등록:
 *   - AlarmReceiver.onReceive() → AlarmHelper.reRegisterAlarm() 호출
 *   - 다음 주 동일 슬롯으로 재등록 (주간 반복)
 */
public class AlarmHelper {

    private static final String PREFS_NAME = "alarm_slots";
    private static final String DIVIDER    = "||";

    // ──────────────────────────────────────────────
    // 슬롯 키 / requestCode 생성
    // ──────────────────────────────────────────────

    /** 슬롯 키: "slot_{dayIndex}_{timeType}_{HHmm}" (예: "slot_0_0_0800") */
    private static String slotKey(int dayIndex, int timeType, String time) {
        return "slot_" + dayIndex + "_" + timeType + "_" + time.replace(":", "");
    }

    /** 슬롯 requestCode: 슬롯 키 해시값 (같은 슬롯은 항상 동일한 코드)
     *  Math.abs(Integer.MIN_VALUE) == MIN_VALUE (음수) 버그 방지 → 최상위 부호 비트 제거 */
    private static int slotRequestCode(String key) {
        return key.hashCode() & 0x7FFFFFFF;
    }

    // ──────────────────────────────────────────────
    // SharedPreferences 약 목록 관리
    // ──────────────────────────────────────────────

    /** 약 항목 문자열: "medicineNo||medicineName||intakeType" */
    private static String toEntry(String medicineNo, String medicineName, String intakeType) {
        return medicineNo + DIVIDER + medicineName + DIVIDER + intakeType;
    }

    /** SharedPreferences에서 슬롯의 약 목록 읽기 (수정 가능한 복사본 반환) */
    public static Set<String> getMedicineSet(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return new HashSet<>(prefs.getStringSet(key, new HashSet<>()));
    }

    /** SharedPreferences에 슬롯의 약 목록 저장 */
    private static void saveMedicineSet(Context context, String key, Set<String> set) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (set.isEmpty()) {
            prefs.edit().remove(key).apply();
        } else {
            prefs.edit().putStringSet(key, set).apply();
        }
    }

    // ──────────────────────────────────────────────
    // AlarmManager 등록 / 취소
    // ──────────────────────────────────────────────

    /**
     * 알람 등록
     *
     * @param context      컨텍스트
     * @param medicineName 약 이름 (알림 표시용)
     * @param medicineNo   약 번호 (슬롯 내 약 구분용)
     * @param weekday      요일 문자열 (쉼표 구분 가능, 예: "0,2,4" or "1")
     * @param timeType     시간대 인덱스 (0:아침, 1:점심, 2:저녁, 3:취침 전)
     * @param intakeType   식전/식후
     * @param time         복용 시간 (HH:mm)
     */
    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarm(Context context, String medicineName, String medicineNo,
                                String weekday, int timeType, String intakeType, String time) {

        if (time == null || time.isEmpty()) return;

        String[] timeParts = time.split(":");
        if (timeParts.length < 2) {
            Log.e("AlarmHelper", "잘못된 time 형식: " + time);
            return;
        }
        int hour   = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        String[] days = weekday.split(",");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (String day : days) {
            int    dayIndex    = Integer.parseInt(day.trim());
            int    calendarDay = convertToCalendarDay(dayIndex);
            String key         = slotKey(dayIndex, timeType, time);

            // 1. SharedPreferences에 이 약을 슬롯에 추가
            Set<String> medicines = getMedicineSet(context, key);
            medicines.add(toEntry(medicineNo, medicineName, intakeType));
            saveMedicineSet(context, key, medicines);

            // 2. AlarmManager에 슬롯 알람 등록 (이미 등록된 슬롯도 갱신 — FLAG_UPDATE_CURRENT)
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            registerAlarmManager(context, alarmManager, key, dayIndex, timeType, time, calendar);
            Log.d("AlarmHelper", "알람 설정됨: " + medicineName + " / 슬롯: " + key);
        }
    }

    /**
     * AlarmReceiver 발동 후 다음 주 동일 슬롯으로 재등록
     * SharedPreferences는 수정하지 않음 (약 목록은 그대로 유지)
     */
    @SuppressLint("ScheduleExactAlarm")
    public static void reRegisterAlarm(Context context, String key,
                                       String weekday, int timeType, String time) {
        String[] timeParts = time.split(":");
        if (timeParts.length < 2) {
            Log.e("AlarmHelper", "reRegisterAlarm - 잘못된 time 형식: " + time);
            return;
        }
        int hour   = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        int dayIndex    = Integer.parseInt(weekday.trim());
        int calendarDay = convertToCalendarDay(dayIndex);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendarDay);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.add(Calendar.WEEK_OF_YEAR, 1); // 다음 주로 설정

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        registerAlarmManager(context, alarmManager, key, dayIndex, timeType, time, calendar);
        Log.d("AlarmHelper", "알람 재등록됨: 슬롯=" + key);
    }

    /** AlarmManager에 PendingIntent 등록 */
    @SuppressLint("ScheduleExactAlarm")
    private static void registerAlarmManager(Context context, AlarmManager alarmManager,
                                             String key, int dayIndex, int timeType,
                                             String time, Calendar calendar) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("slot_key",        key);
        intent.putExtra("weekday",         String.valueOf(dayIndex));
        intent.putExtra("time",            time);
        intent.putExtra("time_type_index", timeType);
        intent.putExtra("time_type",       getTimeTypeName(timeType));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                slotRequestCode(key),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    /**
     * 특정 요일/시간대의 약 1개 취소
     * - SharedPreferences에서 해당 약만 제거
     * - 슬롯이 비면 AlarmManager 알람도 취소
     *
     * @param medicineNo 약 번호
     * @param weekday    요일 (단일 값, 예: "1")
     * @param timeType   시간대 인덱스 (0~3)
     */
    public static void cancelAlarm(Context context, String medicineNo,
                                   String weekday, int timeType) {
        int    dayIndex   = Integer.parseInt(weekday.trim());
        String keyPrefix  = "slot_" + dayIndex + "_" + timeType + "_";

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, ?> mapEntry : allEntries.entrySet()) {
            String key = mapEntry.getKey();
            if (!key.startsWith(keyPrefix)) continue;

            //noinspection unchecked
            Set<String> medicines = new HashSet<>((Set<String>) mapEntry.getValue());
            boolean changed = medicines.removeIf(e -> e.startsWith(medicineNo + DIVIDER));
            if (!changed) continue;

            if (medicines.isEmpty()) {
                cancelAlarmManager(context, key);
                editor.remove(key);
                Log.d("AlarmHelper", "슬롯 비어 알람 취소: " + key);
            } else {
                editor.putStringSet(key, medicines);
                Log.d("AlarmHelper", "약 제거됨 (슬롯 유지): " + medicineNo + " / 슬롯: " + key);
            }
        }
        editor.apply(); // 루프 완료 후 한 번만 apply
    }

    /**
     * 특정 약의 모든 알람 취소 (약 삭제 시 호출)
     * - SharedPreferences 전체에서 해당 약 항목 제거
     * - 슬롯이 비면 AlarmManager 알람도 취소
     *
     * @param medicineNo 약 번호
     */
    public static void cancelAllAlarms(Context context, String medicineNo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, ?> mapEntry : allEntries.entrySet()) {
            String key = mapEntry.getKey();
            if (!key.startsWith("slot_")) continue;

            //noinspection unchecked
            Set<String> medicines = new HashSet<>((Set<String>) mapEntry.getValue());
            boolean changed = medicines.removeIf(e -> e.startsWith(medicineNo + DIVIDER));
            if (!changed) continue;

            if (medicines.isEmpty()) {
                cancelAlarmManager(context, key);
                editor.remove(key);
            } else {
                editor.putStringSet(key, medicines);
            }
        }
        editor.apply();
        Log.d("AlarmHelper", "전체 알람 취소됨: medicineNo=" + medicineNo);
    }

    /**
     * 모든 슬롯의 알람 전체 취소 (로그아웃 시 호출)
     * - SharedPreferences의 모든 슬롯 삭제
     * - AlarmManager에 등록된 모든 슬롯 알람 취소
     */
    public static void cancelAllAlarmsForAllMedicines(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = prefs.getAll();

        for (Map.Entry<String, ?> mapEntry : allEntries.entrySet()) {
            String key = mapEntry.getKey();
            if (key.startsWith("slot_")) {
                cancelAlarmManager(context, key);
            }
        }

        prefs.edit().clear().apply();
        Log.d("AlarmHelper", "모든 알람 전체 취소됨 (로그아웃)");
    }

    /** AlarmManager에서 슬롯 알람 취소 */
    private static void cancelAlarmManager(Context context, String key) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                slotRequestCode(key),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    // ──────────────────────────────────────────────
    // 유틸
    // ──────────────────────────────────────────────

    /** 앱 내부 요일 인덱스 → Calendar 요일 상수 변환 (0=월 ~ 6=일) */
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

    /** 시간대 인덱스 → 한글 이름 변환 */
    public static String getTimeTypeName(int timeType) {
        switch (timeType) {
            case 0: return "아침";
            case 1: return "점심";
            case 2: return "저녁";
            case 3: return "취침 전";
            default: return "";
        }
    }
}
