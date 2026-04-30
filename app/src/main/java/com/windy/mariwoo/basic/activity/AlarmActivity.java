package com.windy.mariwoo.basic.activity;

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

import com.windy.mariwoo.R;
import com.windy.mariwoo.basic.LoginActivity;

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ✅ 한 번만 있으면 돼
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "mariwoo:AlarmWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L);

        setContentView(R.layout.activity_alarm);
        handleIntent(getIntent());
    }

    // ✅ 이미 Activity가 떠있을 때 새 알람 오면 여기서 처리
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    // ✅ intent 처리 공통 메서드
    private void handleIntent(Intent intent) {
        if (intent == null) {
            Log.d("AlarmActivity", "intent null!");
            return;
        }
        String medicineName = intent.getStringExtra("medicine_name");
        String intakeType   = intent.getStringExtra("intake_type");
        String timeType     = intent.getStringExtra("time_type");

        Log.d("AlarmActivity", "medicineName: " + medicineName);
        Log.d("AlarmActivity", "intakeType: " + intakeType);
        Log.d("AlarmActivity", "timeType: " + timeType);

        TextView tvTitle         = findViewById(R.id.alarmActivity_text_title);
        TextView tvInfo          = findViewById(R.id.alarmActivity_text_info);
        AppCompatButton btnConfirm = findViewById(R.id.alarmActivity_button_confirm);

        tvTitle.setText("💊 약 복용 시간입니다!");
        tvInfo.setText(timeType + " " + intakeType + "\n" + medicineName);

        btnConfirm.setOnClickListener(v -> {
            // ✅ 알림 전체 취소
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancelAll();

            Intent intent2 = new Intent(AlarmActivity.this, LoginActivity.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent2);
            finish();
        });
    }
}