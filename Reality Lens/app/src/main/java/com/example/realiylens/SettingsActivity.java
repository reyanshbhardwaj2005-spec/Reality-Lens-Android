package com.example.realiylens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        LinearLayout btnBack = findViewById(R.id.ll_back_dashboard);
        CardView btnLogout = findViewById(R.id.cv_logout);
        Switch switchDarkMode = findViewById(R.id.switchDarkMode);

        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().remove("access_token").apply();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Dark mode switch logic can be added here if needed
    }
}
