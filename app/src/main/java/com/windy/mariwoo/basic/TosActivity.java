package com.windy.mariwoo.basic;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;

public class TosActivity extends AppCompatActivity {

    private ImageView imgBack;
    private TextView tvTitle;

    private String type;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        type = getIntent().getStringExtra("type");

        tvTitle = findViewById(R.id.tosActivity_textView_title);
        if("use".equals(type)) {
            tvTitle.setText("이용약관 조회");
        }
        else if("privacy".equals(type)) {
            tvTitle.setText("개인정보 취급방침 조회");
        }
        else if("marketing".equals(type)) {
            tvTitle.setText("마케팅 이용약관 조회");
        }

        imgBack = findViewById(R.id.tosActivity_imageView_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}