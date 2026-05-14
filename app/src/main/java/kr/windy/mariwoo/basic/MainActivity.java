package kr.windy.mariwoo.basic;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import kr.windy.mariwoo.R;
import kr.windy.mariwoo.databinding.ActivityMainBinding;

/**
 * 메인 Activity (Navigation Drawer 기반)
 *
 * 역할:
 *   - Drawer 메뉴를 통해 각 Fragment로 이동
 *   - 헤더 영역의 로그아웃 버튼 처리
 *   - 알림 권한 요청 (Android 13+)
 *   - 전체 화면 알림 권한 요청 (Android 14+)
 *
 * 탐색 가능한 Fragment 목록 (nav_graph):
 *   - nav_medicine_list        : 내 약 목록
 *   - nav_medicine_intake_list : 복용 체크
 *   - nav_relation_list        : 관계 목록
 *   - nav_relation_intake_list : 가족 복용 내역 조회
 *   - nav_change_info          : 내 정보 변경
 *   - nav_change_pw            : 비밀번호 변경
 *   - nav_medicine_calendar    : 복용 달력
 */
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration; // 상단 앱 바 + Drawer 연결 설정
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        // FAB 클릭 (현재 미사용 - 향후 기능 확장용)
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });

        DrawerLayout     drawer         = binding.drawerLayout;
        NavigationView   navigationView = binding.navView;

        // 각 메뉴 항목이 탑레벨 목적지로 동작하도록 설정 (뒤로가기 버튼 대신 햄버거 메뉴 표시)
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_medicine_list,
                R.id.nav_medicine_intake_list,
                R.id.nav_relation_list,
                R.id.nav_relation_intake_list,
                R.id.nav_change_info,
                R.id.nav_change_pw,
                R.id.nav_medicine_calendar)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Drawer 헤더 영역 처리
        View     headerView  = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navHeaderMain_textView_name);
        navUserName.setText("test"); // TODO: SharedPreferences에서 실제 사용자 이름으로 설정

        // 로그아웃 버튼 → SharedPreferences 초기화 후 LoginActivity로 이동
        Button btnLogout = headerView.findViewById(R.id.header_btn_logout);
        btnLogout.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("autoLogin", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear(); // 자동 로그인 정보 전체 삭제
            editor.apply();

            // 로그인 화면으로 이동 (백스택 전부 제거하여 뒤로가기 방지)
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 권한 요청은 SplashActivity에서 처리됨
    }

    /**
     * 앱 바의 "위로 이동" 버튼 처리 (Navigation Drawer와 연동)
     */
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
