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

public class AlarmHelper {

    // 요일별 알람 설정
    // dayOfWeek: 0=월 1=화 2=수 3=목 4=금 5=토 6=일
    // timeType:  0=아침 1=점심 2=저녁 3=취침전
    @SuppressLint("ScheduleExactAlarm")
    public static void setAlarm(Context context, String medicineName, String medicineNo,
                                String weekday, int timeType, String intakeType, String time) {

        if (time == null || time.isEmpty()) return;

        // 시간 파싱 (HH:mm)
        String[] timeParts = time.split(":");
        int hour   = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // 선택된 요일 파싱 (예: "0,2,4")
        String[] days = weekday.split(",");

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        String timeTypeName = getTimeTypeName(timeType);

        for (String day : days) {
            int dayIndex = Integer.parseInt(day.trim());

            // Calendar 요일 변환 (0=월요일 → Calendar.MONDAY=2)
            int calendarDay = convertToCalendarDay(dayIndex);

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_WEEK, calendarDay);
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);

            // 이미 지난 시간이면 다음 주로
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            }

            // 고유한 requestCode 생성 (약번호 + 요일 + 시간타입)
            int requestCode = Math.abs((medicineNo + dayIndex + timeType).hashCode());

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("medicine_name", medicineName);
            intent.putExtra("intake_type", intakeType);
            intent.putExtra("time_type", timeTypeName);
            intent.putExtra("weekday", String.valueOf(dayIndex)); // 재등록용
            intent.putExtra("time", time);                        // 재등록용
            intent.putExtra("time_type_index", timeType);         // 재등록용

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // setExactAndAllowWhileIdle: Doze 모드에서도 정확히 울림 (API 23+)
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
    public static void cancelAlarm(Context context, String medicineNo, String weekday, int timeType) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        int dayIndex = Integer.parseInt(weekday.trim());

        // setAlarm과 동일한 requestCode 생성 (약번호 기반)
        int requestCode = Math.abs((medicineNo + dayIndex + timeType).hashCode());

        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();

        Log.d("AlarmHelper", "알람 취소됨: medicineNo=" + medicineNo + " / 요일:" + dayIndex + " / 시간타입:" + timeType);
    }

    public static void cancelAllAlarms(Context context, String medicineNo) {
        for (int day = 0; day <= 6; day++) {
            for (int timeType = 0; timeType <= 3; timeType++) {
                cancelAlarm(context, medicineNo, String.valueOf(day), timeType);
            }
        }
        Log.d("AlarmHelper", "전체 알람 취소됨: medicineNo=" + medicineNo);
    }
    // 0:월 ~ 6:일 → Calendar 요일 변환
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