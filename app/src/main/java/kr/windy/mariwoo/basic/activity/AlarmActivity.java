package kr.windy.mariwoo.basic.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.basic.MainActivity;

import java.util.ArrayList;

/**
 * 약 복용 알람 Activity
 *
 * 역할:
 *   - AlarmReceiver → AlarmService → 이 Activity 순서로 실행됨
 *   - 잠금 화면 위에 전체 화면으로 표시 (WakeLock + FLAG_SHOW_WHEN_LOCKED)
 *   - 약 이름, 복용 시간대(아침/점심 등), 식전/식후 정보 표시
 *   - "확인" 버튼 클릭 → 모든 알림 취소 후 LoginActivity로 이동
 *
 * Intent 수신 데이터:
 *   - medicine_name : 약 이름 (예: "타이레놀")
 *   - intake_type   : 식전/식후
 *   - time_type     : 아침/점심/저녁/취침 전
 *
 * 주의:
 *   - AlarmActivity가 이미 떠있을 때 새 알람이 오면 onNewIntent()에서 처리
 *   - WakeLock은 최대 10분 유지 후 자동 해제
 */
public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 잠금 화면 위에 표시 + 화면 켜기
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Android 8.1 이상: API 방식으로 설정
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            // Android 8.0 이하: Flag 방식으로 설정
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        // WakeLock 획득: 화면을 최대 10분간 켜둠 (알람 확인을 위해)
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "mariwoo:AlarmWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L); // 최대 10분 유지

        setContentView(R.layout.activity_alarm);
        handleIntent(getIntent());
    }

    /**
     * 이미 Activity가 실행 중일 때 새 알람 Intent가 오면 여기서 처리
     * (예: 아침 알람 Activity가 떠있는데 점심 알람이 울리는 경우)
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    /**
     * Intent에서 알람 정보를 꺼내 UI에 표시
     * "확인" 버튼 클릭 시 모든 알림을 취소하고 LoginActivity로 이동
     *
     * @param intent AlarmService에서 전달받은 Intent
     */
    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d("AlarmActivity", "intent null!");
            return;
        }

        // Intent에서 약 목록과 시간대 정보 추출
        ArrayList<String> medicineNames = intent.getStringArrayListExtra("medicine_names");
        String            timeType      = intent.getStringExtra("time_type");

        Log.d("AlarmActivity", "시간대: " + timeType + " / 약 수: "
                + (medicineNames != null ? medicineNames.size() : 0));

        // UI 표시
        TextView         tvTitle    = findViewById(R.id.alarmActivity_text_title);
        TextView         tvInfo     = findViewById(R.id.alarmActivity_text_info);
        AppCompatButton  btnConfirm = findViewById(R.id.alarmActivity_button_confirm);

        tvTitle.setText("💊 약 복용 시간입니다!");

        // 시간대 + 약 목록 표시 (여러 약이면 bullet 목록으로)
        StringBuilder sb = new StringBuilder();
        if (timeType != null) sb.append(timeType).append("\n\n");
        if (medicineNames != null) {
            for (String name : medicineNames) {
                sb.append("• ").append(name).append("\n");
            }
        }
        tvInfo.setText(sb.toString().trim());

        // 확인 버튼 → 알림 전체 취소 후 복용 체크 화면으로 이동
        btnConfirm.setOnClickListener(v -> {
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancelAll(); // 모든 알림 취소

            Intent intent2 = new Intent(AlarmActivity.this, MainActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent2.putExtra("navigate_to", R.id.nav_medicine_intake_list);
            startActivity(intent2);
            finish();
        });
    }
}
