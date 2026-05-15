package kr.windy.mariwoo.basic.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.activity.AlarmActivity;
import kr.windy.mariwoo.basic.helper.AlarmHelper;

import java.util.ArrayList;
import java.util.Set;

/**
 * 약 복용 알람 수신 BroadcastReceiver
 *
 * 동작 흐름:
 *   1. AlarmHelper.setAlarm()이 등록한 알람 시간이 되면 onReceive() 호출
 *   2. SharedPreferences에서 해당 슬롯의 약 목록 전체를 읽어 AlarmActivity에 전달
 *   3. 같은 시간에 여러 약이 있어도 AlarmActivity를 한 번만 실행 (묶어서 표시)
 *   4. 다음 주 동일 슬롯으로 알람 재등록 (주간 반복)
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medicine_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {

        // 슬롯 정보 꺼내기
        String slotKey       = intent.getStringExtra("slot_key");       // SharedPreferences 키
        String weekday       = intent.getStringExtra("weekday");        // 요일 (재등록용)
        String time          = intent.getStringExtra("time");           // HH:mm (재등록용)
        int    timeTypeIndex = intent.getIntExtra("time_type_index", 0); // 시간대 인덱스 (재등록용)
        String timeTypeName  = intent.getStringExtra("time_type");      // 아침/점심/저녁/취침 전 (표시용)

        Log.d("AlarmReceiver", "알람 수신됨! 슬롯: " + slotKey);

        // slotKey가 null이면 SharedPreferences 조회 불가 → 무시
        if (slotKey == null) {
            Log.e("AlarmReceiver", "slotKey가 null, 알람 무시");
            return;
        }

        // SharedPreferences에서 이 슬롯의 약 목록 읽기
        // 형식: Set<"medicineNo||medicineName||intakeType">
        Set<String> medicineSet = AlarmHelper.getMedicineSet(context, slotKey);

        // AlarmActivity에 넘길 표시용 문자열 목록 구성: "약이름 (식전/식후)"
        ArrayList<String> displayList = new ArrayList<>();
        StringBuilder     notifText   = new StringBuilder();

        for (String entry : medicineSet) {
            String[] parts = entry.split("\\|\\|");
            if (parts.length < 3) continue;
            String name   = parts[1];
            String intake = parts[2];
            displayList.add(name + " (" + intake + ")");
            if (notifText.length() > 0) notifText.append(", ");
            notifText.append(name);
        }

        if (displayList.isEmpty()) {
            Log.d("AlarmReceiver", "슬롯에 약이 없음, 알람 무시");
            return;
        }

        // AlarmActivity 실행 Intent
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putStringArrayListExtra("medicine_names", displayList); // ArrayList<String>
        alarmIntent.putExtra("time_type", timeTypeName);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        // fullScreenIntent용 PendingIntent
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 오버레이 권한 있으면 AlarmActivity 직접 실행
        if (android.provider.Settings.canDrawOverlays(context)) {
            context.startActivity(alarmIntent);
        }

        // 알림 채널 생성 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "약 복용 알림", NotificationManager.IMPORTANCE_HIGH);
            channel.setShowBadge(true);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // 알림 표시 텍스트 (여러 약이면 쉼표로 나열)
        String notifBody = timeTypeName + " - " + notifText.toString();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("💊 약 복용 시간입니다!")
                .setContentText(notifBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notifBody)) // 긴 텍스트 펼침
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());

        // 다음 주 동일 슬롯으로 알람 재등록 (약 목록은 SharedPreferences에서 그대로 유지)
        if (slotKey != null && weekday != null && time != null) {
            AlarmHelper.reRegisterAlarm(context, slotKey, weekday, timeTypeIndex, time);
        }
    }
}
