package com.windy.mariwoo.basic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.windy.mariwoo.R;

public class SignActivity extends AppCompatActivity {

    private ImageView imgBack;

    private CheckBox chkUse;
    private CheckBox chkPrivacy;
    private CheckBox chkMarketing;

    private TextView tvUse;
    private TextView tvPrivacy;
    private TextView tvMarketing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        imgBack = findViewById(R.id.signActivity_imageView_back);
        imgBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvUse = findViewById(R.id.signActivity_textView_use);
        tvUse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "use");
                startActivity(intent);
            }
        });

        tvPrivacy = findViewById(R.id.signActivity_textView_privacy);
        tvPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "privacy");
                startActivity(intent);
            }
        });

        tvMarketing = findViewById(R.id.signActivity_textView_marketing);
        tvMarketing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignActivity.this, TosActivity.class);
                intent.putExtra("type", "marketing");
                startActivity(intent);
            }
        });
    }
}