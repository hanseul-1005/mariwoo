package com.windy.mariwoo.basic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.activity.AlarmActivity;

/**
 * 약 복용 알람 Foreground Service
 *
 * 역할:
 *   - AlarmReceiver에서 startForegroundService()로 호출됨
 *   - Android 백그라운드 Activity 실행 제한을 우회하기 위해 Foreground Service로 실행
 *   - Foreground 상태로 전환 후 AlarmActivity를 실행하고 즉시 종료
 *
 * 동작 흐름:
 *   AlarmReceiver → AlarmService (Foreground) → AlarmActivity
 *
 * 주의:
 *   - Foreground Service는 반드시 알림(Notification)을 표시해야 함
 *   - AlarmActivity 실행 후 stopSelf()로 즉시 Service 종료
 *   - START_NOT_STICKY: 시스템에 의해 종료된 경우 재시작하지 않음
 */
public class AlarmService extends Service {

    // Foreground Service용 알림 채널 ID
    private static final String CHANNEL_ID = "medicine_alarm_service_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Intent에서 알람 정보 추출
        String medicineName = intent.getStringExtra("medicine_name"); // 약 이름
        String intakeType   = intent.getStringExtra("intake_type");   // 식전/식후
        String timeType     = intent.getStringExtra("time_type");     // 아침/점심/저녁/취침 전

        // Foreground Service용 알림 채널 생성 (Android 8.0 이상 필수)
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "알람 서비스",
                    NotificationManager.IMPORTANCE_LOW // 소리 없는 알림
            );
            manager.createNotificationChannel(channel);
        }

        // Foreground Service 필수 알림 생성 (상태바에 표시됨)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("약 복용 알림")
                .build();

        startForeground(1, notification); // Foreground Service로 전환

        // Foreground Service에서 Activity 실행 (백그라운드 실행 제한 우회)
        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.putExtra("medicine_name", medicineName);
        alarmIntent.putExtra("intake_type",   intakeType);
        alarmIntent.putExtra("time_type",     timeType);
        alarmIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |        // 새 Task에서 Activity 실행
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION // 사용자 동작 없이 실행
        );
        startActivity(alarmIntent);

        stopSelf(); // AlarmActivity 실행 후 Service 즉시 종료
        return START_NOT_STICKY; // 시스템 종료 시 재시작 불필요
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Bound Service 미사용
    }
}
