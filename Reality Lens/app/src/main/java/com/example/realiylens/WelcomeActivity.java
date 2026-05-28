package com.example.realiylens;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        View skeleton = findViewById(R.id.welcome_skeleton);
        View content = findViewById(R.id.welcome_content);

        // Show skeleton initially
        skeleton.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);

        // Simulate a small loading delay for the transition effect
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            // Add a simple fade-in animation
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(500).start();
        }, 1200);

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
