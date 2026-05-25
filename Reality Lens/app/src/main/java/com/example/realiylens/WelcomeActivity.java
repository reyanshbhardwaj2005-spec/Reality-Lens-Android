package com.example.realiylens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        Button btnOpenDashboard = findViewById(R.id.btn_open_dashboard);
        Button btnMinimize = findViewById(R.id.btn_minimize);

        btnOpenDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, DashboardActivity.class);
            startActivity(intent);
        });

        btnMinimize.setOnClickListener(v -> {
            moveTaskToBack(true);
        });
    }
}
