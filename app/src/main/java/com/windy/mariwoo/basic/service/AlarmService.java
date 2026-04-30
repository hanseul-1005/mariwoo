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

public class AlarmService extends Service {

    private static final String CHANNEL_ID = "medicine_alarm_service_channel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String medicineName = intent.getStringExtra("medicine_name");
        String intakeType   = intent.getStringExtra("intake_type");
        String timeType     = intent.getStringExtra("time_type");

        // ✅ Foreground Service 알림 (필수)
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "알람 서비스", NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("약 복용 알림")
                .build();

        startForeground(1, notification);

        // ✅ Foreground Service에서 Activity 실행 (백그라운드 제한 우회)
        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.putExtra("medicine_name", medicineName);
        alarmIntent.putExtra("intake_type", intakeType);
        alarmIntent.putExtra("time_type", timeType);
        alarmIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
        );
        startActivity(alarmIntent);

        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}