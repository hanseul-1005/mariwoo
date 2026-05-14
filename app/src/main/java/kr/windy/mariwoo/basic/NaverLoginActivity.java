package kr.windy.mariwoo.basic;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.navercorp.nid.NaverIdLoginSDK;
import com.navercorp.nid.oauth.OAuthLoginCallback;
import com.navercorp.nid.oauth.view.NidOAuthLoginButton;
import com.windy.mariwoo.R;

public class NaverLoginActivity extends AppCompatActivity {

    private TextView textView;
    private NidOAuthLoginButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_naver_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textView = findViewById(R.id.naverLoginActivity_textView);

        //네아로 객체 초기화
        NaverIdLoginSDK.INSTANCE.initialize(this, getString(R.string.naver_client_id),
                getString(R.string.naver_client_secret), getString(R.string.app_name));

        button = findViewById(R.id.naverLoginActivity_button);
        button.setOAuthLogin(new OAuthLoginCallback() {
            @Override
            public void onSuccess() {
                // 로그인 성공시
                // 액세스 토큰 가져오기
                String accessToken = NaverIdLoginSDK.INSTANCE.getAccessToken();
                textView.setText(accessToken);
                //btnLogin.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(int httpStatus, @NonNull String message) {
                // 통신 오류
                Log.e("네아로", "onFailure: httpStatus - " + httpStatus + " / message - " + message);
            }

            @Override
            public void onError(int errorCode, @NonNull String message) {
                // 네이버 로그인 중 오류 발생
                Log.e("네아로", "onError: errorCode - " + errorCode + " / message - " + message);
            }
        });
    }

}