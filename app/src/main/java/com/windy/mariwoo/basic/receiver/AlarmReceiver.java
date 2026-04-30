package com.windy.mariwoo.basic.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.activity.AlarmActivity;
import com.windy.mariwoo.basic.helper.AlarmHelper;
import com.windy.mariwoo.basic.service.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_alarm_channel";
    @Override
    public void onReceive(Context context, Intent intent) {
        String medicineName  = intent.getStringExtra("medicine_name");
        String intakeType    = intent.getStringExtra("intake_type");
        String timeType      = intent.getStringExtra("time_type");
        String weekday       = intent.getStringExtra("weekday");
        String time          = intent.getStringExtra("time");
        int    timeTypeIndex = intent.getIntExtra("time_type_index", 0);

        Log.d("AlarmReceiver", "알람 수신됨! " + medicineName);

        // ✅ AlarmActivity fullScreenIntent
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra("medicine_name", medicineName);
        alarmIntent.putExtra("intake_type", intakeType);
        alarmIntent.putExtra("time_type", timeType);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // ✅ 다른 앱 위에 표시 권한 있으면 직접 실행
        if (android.provider.Settings.canDrawOverlays(context)) {
            context.startActivity(alarmIntent);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "약 복용 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setShowBadge(true);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("💊 약 복용 시간입니다!")
                .setContentText(timeType + " " + intakeType + " - " + medicineName)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true) // ✅ 핵심
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());

        // ✅ 다음 주 재등록
        if (medicineName != null && weekday != null && time != null) {
            AlarmHelper.setAlarm(context, medicineName, weekday, timeTypeIndex, intakeType, time);
        }
    }

}