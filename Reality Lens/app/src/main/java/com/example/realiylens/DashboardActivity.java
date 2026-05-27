package com.example.realiylens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Dashboard_Binding";
    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressBar;
    private LinearLayout llVerificationsContainer;

    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.example.realiylens.ANALYSIS_STARTED".equals(action)) {
                if (progressBar != null) progressBar.show();
            } else if ("com.example.realiylens.ANALYSIS_FINISHED".equals(action)) {
                if (progressBar != null) progressBar.hide();
                fetchHistory();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        drawerLayout = findViewById(R.id.drawer_layout);
        progressBar = findViewById(R.id.pb_dashboard_loading);
        llVerificationsContainer = findViewById(R.id.ll_verifications_container);
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.realiylens.ANALYSIS_STARTED");
        filter.addAction("com.example.realiylens.ANALYSIS_FINISHED");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
        
        checkAnalysisStatus();
        fetchHistory();
    }

    private void logout() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().remove("access_token").apply();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void checkAnalysisStatus() {
        if (getSharedPreferences("AppPrefs", MODE_PRIVATE).getBoolean("is_analyzing", false)) {
            if (progressBar != null) progressBar.show();
        } else {
            if (progressBar != null) progressBar.hide();
        }
    }

    private void fetchHistory() {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("access_token", "");
        if (token.isEmpty()) {
            Log.e(TAG, "Access token is missing");
            return;
        }

        if (progressBar != null) progressBar.show();

        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<List<ResultResponse>>() {
            @Override
            public void onResponse(Call<List<ResultResponse>> call, Response<List<ResultResponse>> response) {
                if (progressBar != null) progressBar.hide();
                
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "History successfully fetched: " + response.body().size() + " items");
                    populateHistory(response.body());
                } else if (response.code() == 401) {
                    logout();
                } else {
                    Log.e(TAG, "History API error: " + response.code());
                    Toast.makeText(DashboardActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ResultResponse>> call, Throwable t) {
                if (progressBar != null) progressBar.hide();
                Log.e(TAG, "History network failure: " + t.getMessage());
            }
        });
    }

    private void populateHistory(List<ResultResponse> history) {
        if (llVerificationsContainer == null) {
            Log.e(TAG, "llVerificationsContainer is null");
            return;
        }

        // Clear existing items immediately
        llVerificationsContainer.removeAllViews();
        
        if (history == null || history.isEmpty()) {
            Log.d(TAG, "No history items to display on dashboard");
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (ResultResponse item : history) {
            if (item == null) continue;

            View itemView = inflater.inflate(R.layout.item_recent_verification, llVerificationsContainer, false);

            TextView tvVerdict = itemView.findViewById(R.id.tv_verdict_badge);
            TextView tvStats = itemView.findViewById(R.id.tv_stats);
            TextView tvContent = itemView.findViewById(R.id.tv_content_preview);
            TextView tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ImageView ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);

            // 1. Map API "verdict" field -> tv_verdict_badge
            String verdict = item.getVerdict();
            tvVerdict.setText(verdict != null ? verdict.toUpperCase() : "ANALYZED");
            
            // Apply color coding for verdict badge
            if (verdict != null) {
                String v = verdict.toUpperCase();
                if (v.contains("REAL") || v.contains("TRUE")) {
                    tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.success_green));
                } else if (v.contains("FAKE") || v.contains("FALSE")) {
                    tvVerdict.setTextColor(Color.RED);
                } else {
                    tvVerdict.setTextColor(ContextCompat.getColor(this, R.color.link_color));
                }
            }

            // 2. Map API "claim" field -> tv_content_preview
            String claim = item.getClaim();
            tvContent.setText(claim != null && !claim.isEmpty() ? claim : "No captured text available");

            // 3. Map API "reality_score" field -> tv_stats
            Double score = item.getRealityScore();
            if (score != null) {
                tvStats.setText(String.format(Locale.getDefault(), "Score: %.2f", score));
            } else {
                tvStats.setText("Score: N/A");
            }

            // 4. Map API "created_at" field -> tv_timestamp
            String createdAt = item.getCreatedAt();
            tvTimestamp.setText(createdAt != null ? createdAt : "Recently verified");

            // Load Image Thumbnail using Glide
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(item.getImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivThumbnail);
            }

            // Click listener for details navigation
            itemView.setOnClickListener(v -> {
                if (item.getJobId() != null) {
                    Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                    intent.putExtra("job_id", item.getJobId());
                    startActivity(intent);
                }
            });

            Log.d(TAG, "Binding data for item: " + item.getJobId() + " (Verdict: " + verdict + ")");
            llVerificationsContainer.addView(itemView);
        }
        
        // Force refresh and ensure visibility
        llVerificationsContainer.setVisibility(View.VISIBLE);
        llVerificationsContainer.requestLayout();
        llVerificationsContainer.invalidate();
        Log.d(TAG, "Successfully populated dashboard with " + llVerificationsContainer.getChildCount() + " items");
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAnalysisStatus();
        fetchHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(statusReceiver); } catch (Exception ignored) {}
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
