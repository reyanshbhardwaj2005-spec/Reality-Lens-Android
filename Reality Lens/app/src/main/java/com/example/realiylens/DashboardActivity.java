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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.bumptech.glide.Glide;
import com.example.realiylens.network.MainResponseModel;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.example.realiylens.network.UserResponse;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "Dashboard_Binding";
    private DrawerLayout drawerLayout;
    private LinearProgressIndicator progressBar;
    private LinearLayout llVerificationsContainer;
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
        ImageButton btnMenu = findViewById(R.id.btn_hamburger_menu);
        NavigationView navigationView = findViewById(R.id.nav_view_sidebar);
        
        // Initialize Nav Header Views
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
        
        if (token.isEmpty()) {
            Log.e(TAG, "Access token is missing in SharedPreferences.");
            return;
        }

        if (progressBar != null) progressBar.show();

        RetrofitClient.getApiService().getHistory("Bearer " + token).enqueue(new Callback<List<MainResponseModel>>() {
            @Override
            public void onResponse(Call<List<MainResponseModel>> call, Response<List<MainResponseModel>> response) {
                if (progressBar != null) progressBar.hide();
                
                if (response.isSuccessful() && response.body() != null) {
                    populateHistory(response.body());
                } else if (response.code() == 401) {
                    logout();
                } else {
                    Log.e(TAG, "History API error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<MainResponseModel>> call, Throwable t) {
                if (progressBar != null) progressBar.hide();
                Log.e(TAG, "History fetch network failure: " + t.getMessage());
            }
        });
    }

    private void populateHistory(List<MainResponseModel> history) {
        if (llVerificationsContainer == null) return;
        llVerificationsContainer.removeAllViews();
        if (history == null || history.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        for (MainResponseModel item : history) {
            if (item == null) continue;
            View itemView = inflater.inflate(R.layout.item_recent_verification, llVerificationsContainer, false);

            TextView tvVerdict = itemView.findViewById(R.id.tv_verdict_badge);
            TextView tvContent = itemView.findViewById(R.id.tv_content_preview);
            TextView tvStats = itemView.findViewById(R.id.tv_stats);
            TextView tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ImageView ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);

            ResultResponse result = item.getResult();
            if (result != null) {
                tvVerdict.setText(result.getVerdict() != null ? result.getVerdict().toUpperCase() : "UNKNOWN");
                String claim = result.getClaim();
                tvContent.setText(claim != null && !claim.trim().isEmpty() ? claim : "No captured claim available");
                StringBuilder stats = new StringBuilder();
                if (result.getConfidence() != null) stats.append("Conf: ").append((int)(result.getConfidence() * 100)).append("%");
                if (result.getRealityScore() != null) {
                    if (stats.length() > 0) stats.append(" | ");
                    stats.append("Score: ").append(result.getRealityScore());
                }
                tvStats.setText(stats.toString());
            } else {
                tvVerdict.setText(item.getStatus() != null ? item.getStatus().toUpperCase() : "PENDING");
                tvContent.setText("Analysis in progress...");
                tvStats.setText("");
            }

            tvTimestamp.setText(item.getCreatedAt() != null ? item.getCreatedAt() : "Date & Time");

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(this).load(item.getImageUrl()).placeholder(android.R.drawable.ic_menu_gallery).error(android.R.drawable.ic_menu_gallery).into(ivThumbnail);
            }

            itemView.setOnClickListener(v -> {
                if (item.getId() != null) {
                    Intent intent = new Intent(DashboardActivity.this, VerificationResultActivity.class);
                    intent.putExtra("job_id", item.getId());
                    startActivity(intent);
                }
            });
            llVerificationsContainer.addView(itemView);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAnalysisStatus();
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
