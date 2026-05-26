package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.realiylens.network.LoginResponse;
import com.example.realiylens.network.RegisterRequest;
import com.example.realiylens.network.RetrofitClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPassword;
    private Button btnContinue, btnGoogle;
    private androidx.appcompat.widget.AppCompatButton btnLoginHere;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnContinue = findViewById(R.id.btn_continue);
        btnGoogle = findViewById(R.id.btn_google_signup);
        btnLoginHere = findViewById(R.id.tv_login_here);

        // Set up click listeners
        btnContinue.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show();
            } else {
                performRegistration(name, email, password);
            }
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-Up clicked", Toast.LENGTH_SHORT).show();
        });

        btnLoginHere.setOnClickListener(v -> {
            // Return to LoginActivity
            finish();
        });
    }

    private void performRegistration(String name, String email, String password) {
        btnContinue.setEnabled(false);

        RegisterRequest registerRequest = new RegisterRequest(name, email, password);

        RetrofitClient.getApiService().register(registerRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnContinue.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();

                    // Store token in SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    prefs.edit().putString("access_token", token).apply();

                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                    // Navigate to WelcomeActivity
                    Intent intent = new Intent(RegisterActivity.this, WelcomeActivity.class);
                    startActivity(intent);
                    finishAffinity(); // Close login and register activities
                } else {
                    Toast.makeText(RegisterActivity.this, "Registration failed: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnContinue.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Network failure: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
