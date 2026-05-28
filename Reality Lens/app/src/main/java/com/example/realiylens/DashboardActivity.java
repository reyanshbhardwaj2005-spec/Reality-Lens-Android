package com.example.realiylens;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.UserResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Dashboard_Binding";
    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressBar;
    private LinearLayout llVerificationsContainer;
    private LinearLayout llSkeletonContainer;
    private TextView tvUserUsername, tvUserEmail;

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
        llSkeletonContainer = findViewById(R.id.ll_skeleton_container);
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);
        
        View headerView = navigationView.getHeaderView(0);
        tvUserUsername = headerView.findViewById(R.id.tv_user_username);
        tvUserEmail = headerView.findViewById(R.id.tv_user_email);

        final boolean[] isRotated = {false};

        btnMenu.setOnClickListener(v -> {
            if (!isRotated[0]) {
                btnMenu.animate().rotation(180f).setDuration(300).start();
            } else {
                btnMenu.animate().rotation(0f).setDuration(300).start();
            }
            isRotated[0] = !isRotated[0];
            drawerLayout.openDrawer(GravityCompat.END);
        });

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                logout();
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.END);
            return false;
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
        fetchUserInfo();
        fetchHistory();
    }

    private void fetchUserInfo() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getUserInfo("Bearer " + token).enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    tvUserUsername.setText(response.body().getUsername());
                    tvUserEmail.setText(response.body().getEmail());
                }
            }
            @Override
            public void onFailure(Call<UserResponse> call, Throwable t) {
                Log.e(TAG, "Failed to fetch user info: " + t.getMessage());
            }
        });
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
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        
        if (token.isEmpty()) return;

        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<List<MainResponseModel>>() {
            @Override
            public void onResponse(Call<List<MainResponseModel>> call, Response<List<MainResponseModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateHistory(response.body());
                } else {
                    if (llSkeletonContainer != null) llSkeletonContainer.setVisibility(View.GONE);
                    if (response.code() == 401) logout();
                }
            }

            @Override
            public void onFailure(Call<List<MainResponseModel>> call, Throwable t) {
                if (llSkeletonContainer != null) llSkeletonContainer.setVisibility(View.GONE);
            }
        });
    }

    private void populateHistory(List<MainResponseModel> history) {
        if (llSkeletonContainer != null) llSkeletonContainer.setVisibility(View.GONE);
        if (llVerificationsContainer == null) return;
        
        llVerificationsContainer.setVisibility(View.VISIBLE);
        llVerificationsContainer.removeAllViews();
        
        if (history == null || history.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (MainResponseModel item : history) {
            View itemView = inflater.inflate(R.layout.item_recent_verification, llVerificationsContainer, false);
            
            TextView tvVerdict = itemView.findViewById(R.id.tv_verdict_badge);
            TextView tvContent = itemView.findViewById(R.id.tv_content_preview);
            TextView tvStats = itemView.findViewById(R.id.tv_stats);
            TextView tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ImageView ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            LinearProgressIndicator pbRealityScore = itemView.findViewById(R.id.pb_item_reality_score);

            // Fetch data with fallbacks (support both flat and nested structures)
            String verdict = item.getVerdict();
            String claim = item.getClaim();
            Double realityScore = item.getRealityScore();
            Double confidence = item.getConfidence();

            if (item.getResult() != null) {
                if (verdict == null) verdict = item.getResult().getVerdict();
                if (claim == null) claim = item.getResult().getClaim();
                if (realityScore == null) realityScore = item.getResult().getRealityScore();
                if (confidence == null) confidence = item.getResult().getConfidence();
            }

            // Set UI components
            tvVerdict.setText(verdict != null ? verdict.toUpperCase() : "ANALYZED");
            applyVerdictColor(tvVerdict, pbRealityScore, verdict);
            tvContent.setText(claim != null ? claim : "No captured claim available");
            
            StringBuilder stats = new StringBuilder();
            if (realityScore != null) {
                stats.append("Score: ").append(String.format(Locale.getDefault(), "%.1f", realityScore));
            }
            if (confidence != null) {
                if (stats.length() > 0) stats.append(" | ");
                stats.append("Conf: ").append((int)(confidence * 100)).append("%");
            }
            tvStats.setText(stats.toString());

            if (realityScore != null && pbRealityScore != null) {
                pbRealityScore.setProgress((int)(realityScore * 100));
            } else if (pbRealityScore != null) {
                pbRealityScore.setProgress(0);
            }

            tvTimestamp.setText(formatDate(item.getCreatedAt()));
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(this).load(item.getImageUrl()).into(ivThumbnail);
            }

            // Click listener to navigate to detailed view
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                intent.putExtra("job_id", item.getId());
                startActivity(intent);
            });

            llVerificationsContainer.addView(itemView);
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "Date & Time";
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat inputFormatWithMs = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());
            
            Date date;
            try {
                date = inputFormat.parse(dateString);
            } catch (Exception e) {
                date = inputFormatWithMs.parse(dateString);
            }

            if (date == null) return dateString;

            SimpleDateFormat outputFormat = new SimpleDateFormat("M/d/yyyy, h:mm:ss a", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + dateString, e);
            return dateString;
        }
    }

    private void applyVerdictColor(TextView tvVerdict, LinearProgressIndicator pbRealityScore, String verdict) {
        if (verdict == null) return;
        int colorRes = R.color.white;
        String v = verdict.toUpperCase();
        
        if (v.contains("LIKELY REAL") || v.equals("REAL")) {
            colorRes = R.color.verdict_real;
        } else if (v.contains("LIKELY FAKE") || v.equals("FAKE")) {
            colorRes = R.color.verdict_fake;
        } else if (v.contains("SUSPICIOUS")) {
            colorRes = R.color.verdict_suspicious;
        } else if (v.contains("UNVERIFIED")) {
            colorRes = R.color.verdict_unverified;
        } else if (v.contains("UNREADABLE")) {
            colorRes = R.color.verdict_unreadable;
        } else if (v.contains("SATIRE")) {
            colorRes = R.color.verdict_satire;
        }
        
        int color = ContextCompat.getColor(this, colorRes);
        tvVerdict.setTextColor(color);
        if (pbRealityScore != null) pbRealityScore.setIndicatorColor(color);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserInfo();
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
