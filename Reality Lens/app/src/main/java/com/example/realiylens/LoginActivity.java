package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.realiylens.network.LoginRequest;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnContinue, btnGoogle, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google);
        btnRegister = findViewById(R.id.btn_register);

        // Set up click listeners
        btnContinue.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                performLogin(email, password);
            }
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In clicked", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Navigate to Registration", Toast.LENGTH_SHORT).show();
        });
    }

    private void performLogin(String email, String password) {
        btnContinue.setEnabled(false);
        
        LoginRequest loginRequest = new LoginRequest(email, password);
        
        RetrofitClient.getApiService().login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnContinue.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    
                    // Store token in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit().putString("access_token", token).apply();

                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to WelcomeActivity
                    Intent intent = new Intent(LoginActivity.this, WelcomeActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginActivity.this, "Invalid credentials or server error", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnContinue.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Network failure: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
