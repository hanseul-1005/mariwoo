package kr.windy.mariwoo.basic.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.MainActivity;

import androidx.core.app.NotificationCompat;

/**
 * 병원 스케줄 단일 알람 수신 BroadcastReceiver
 * - 방문 1시간 전 알림
 * - 반복 없음 (일회성)
 */
public class HospitalAlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "hospital_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String name    = intent.getStringExtra("hospital_name");
        if (name == null) name = "병원";
        String time    = intent.getStringExtra("hospital_time");
        String label   = intent.getStringExtra("hospital_label"); // "24시간" or "1시간"
        int    reqCode = intent.getIntExtra("request_code", 0);

        Log.d("HospitalAlarmReceiver", "병원 알람 수신: " + name + " / " + time);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "병원 스케줄 알림", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        String before = (label != null) ? label : "1시간";
        String body = "[" + name + "] 방문 " + before + " 전입니다.";

        // 알림 탭 시 MainActivity → 병원 스케줄 목록으로 이동
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.putExtra("navigate_to", R.id.nav_hospital_list);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, reqCode, mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("🏥 병원 방문 알림")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setAutoCancel(true);

        manager.notify(reqCode, builder.build());
    }
}
