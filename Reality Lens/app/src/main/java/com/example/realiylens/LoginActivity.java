package com.example.realiylens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
                // Bypass login logic for now
                navigateToMain();
            }
        });

        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In clicked", Toast.LENGTH_SHORT).show();
        });

        btnRegister.setOnClickListener(v -> {
            Toast.makeText(this, "Navigate to Registration", Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
