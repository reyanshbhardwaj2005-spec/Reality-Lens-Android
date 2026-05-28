package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.realiylens.network.LoginRequest;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WelcomeActivity extends AppCompatActivity {

    private View skeleton, content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // If launched as the main entry point, check for persistent login
        String action = getIntent().getStringExtra("action");
        if (action == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            String savedToken = prefs.getString("access_token", null);
            if (savedToken == null || savedToken.isEmpty()) {
                // Not logged in, go to Login
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_welcome);

        skeleton = findViewById(R.id.welcome_skeleton);
        content = findViewById(R.id.welcome_content);

        // Show skeleton immediately
        skeleton.setVisibility(View.VISIBLE);
        content.setVisibility(View.GONE);

        if ("login".equals(action)) {
            String email = getIntent().getStringExtra("email");
            String password = getIntent().getStringExtra("password");
            performLogin(email, password);
        } else {
            // Already logged in or navigating normally
            showContent();
        }

        Button btnOpenDashboard = findViewById(R.id.btn_open_dashboard);
        Button btnMinimize = findViewById(R.id.btn_minimize);

        btnOpenDashboard.setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, DashboardActivity.class));
        });

        btnMinimize.setOnClickListener(v -> {
            moveTaskToBack(true);
        });
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit().putString("access_token", token).apply();
                    showContent();
                } else {
                    Toast.makeText(WelcomeActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(WelcomeActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void showContent() {
        if (skeleton != null && content != null) {
            skeleton.setVisibility(View.GONE);
            content.setVisibility(View.VISIBLE);
            content.setAlpha(0f);
            content.animate().alpha(1f).setDuration(400).start();
        }
    }
}
