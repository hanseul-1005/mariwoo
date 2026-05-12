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

/**
 * 약 복용 알람 수신 BroadcastReceiver
 *
 * 동작 흐름:
 *   1. AlarmHelper.setAlarm()이 등록한 알람 시간이 되면 이 클래스의 onReceive() 호출
 *   2. AlarmActivity를 fullScreenIntent로 띄워 화면 위에 알람 표시
 *   3. 다른 앱 위에 표시 권한이 있으면 Activity를 직접 실행
 *   4. 다음 주 동일 요일/시간에 알람 재등록 (주간 반복 구현)
 */
public class AlarmReceiver extends BroadcastReceiver {

    // 알림 채널 ID (Android 8.0 이상 필수)
    private static final String CHANNEL_ID = "medicine_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {

        // AlarmHelper.setAlarm()에서 putExtra로 담아준 데이터 꺼내기
        String medicineName  = intent.getStringExtra("medicine_name");   // 약 이름 (알림 표시용)
        String medicineNo    = intent.getStringExtra("medicine_no");     // 약 번호 (재등록 requestCode용)
        String intakeType    = intent.getStringExtra("intake_type");     // 식전/식후
        String timeType      = intent.getStringExtra("time_type");       // 아침/점심/저녁/취침 전
        String weekday       = intent.getStringExtra("weekday");         // 요일 (재등록용)
        String time          = intent.getStringExtra("time");            // HH:mm (재등록용)
        int    timeTypeIndex = intent.getIntExtra("time_type_index", 0); // 시간대 인덱스 (재등록용)

        Log.d("AlarmReceiver", "알람 수신됨! " + medicineName);

        // AlarmActivity를 fullScreenIntent 대상으로 설정
        // FLAG_ACTIVITY_NO_USER_ACTION: 사용자 동작 없이 Activity 실행 허용
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra("medicine_name", medicineName);
        alarmIntent.putExtra("intake_type",   intakeType);
        alarmIntent.putExtra("time_type",     timeType);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        // fullScreenIntent용 PendingIntent (잠금화면/다른 앱 위에 표시)
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(), // 유니크한 requestCode
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 다른 앱 위에 표시 권한이 있으면 AlarmActivity 직접 실행
        if (android.provider.Settings.canDrawOverlays(context)) {
            context.startActivity(alarmIntent);
        }

        // Android 8.0 이상: 알림 채널 생성 (채널 없으면 알림 표시 불가)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "약 복용 알림",
                    NotificationManager.IMPORTANCE_HIGH // 소리 + 헤드업 알림
            );
            channel.setShowBadge(true);    // 앱 아이콘 배지 표시
            channel.enableVibration(true); // 진동 활성화
            manager.createNotificationChannel(channel);
        }

        // 알림 빌드
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.icon_medicine)
                .setContentTitle("💊 약 복용 시간입니다!")
                .setContentText(timeType + " " + intakeType + " - " + medicineName)
                .setPriority(NotificationCompat.PRIORITY_MAX)      // 최고 우선순위
                .setCategory(NotificationCompat.CATEGORY_ALARM)    // 알람 카테고리
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 잠금화면에도 표시
                .setFullScreenIntent(fullScreenPendingIntent, true)  // 전체화면 인텐트
                .setAutoCancel(true); // 탭하면 알림 자동 제거

        manager.notify((int) System.currentTimeMillis(), builder.build());

        // 다음 주 동일 요일/시간으로 알람 재등록 (주간 반복)
        // weekday는 단일 요일 값 (AlarmHelper에서 재등록 시 쉼표 구분 없이 단일 값 전달)
        if (medicineName != null && medicineNo != null && weekday != null && time != null) {
            AlarmHelper.setAlarm(context, medicineName, medicineNo,
                    weekday, timeTypeIndex, intakeType, time);
        }
    }
}
