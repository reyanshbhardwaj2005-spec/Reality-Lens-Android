package com.example.realiylens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressBar;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "Received broadcast: " + action);
            if ("com.example.realiylens.ANALYSIS_STARTED".equals(action)) {
                progressBar.show();
            } else if ("com.example.realiylens.ANALYSIS_FINISHED".equals(action)) {
                progressBar.hide();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.pb_dashboard_loading);
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);

        btnMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.END);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        // Register for analysis status updates
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.realiylens.ANALYSIS_STARTED");
        filter.addAction("com.example.realiylens.ANALYSIS_FINISHED");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
        
        checkAnalysisStatus();
    }

    private void checkAnalysisStatus() {
        boolean isAnalyzing = getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("is_analyzing", false);
        Log.d(TAG, "Initial analysis check: " + isAnalyzing);
        if (isAnalyzing) {
            progressBar.show();
        } else {
            progressBar.hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAnalysisStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(statusReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
