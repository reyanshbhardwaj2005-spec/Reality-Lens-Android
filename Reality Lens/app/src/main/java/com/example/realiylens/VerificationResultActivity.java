package com.example.realiylens;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationResultActivity extends AppCompatActivity {

    private static final String TAG = "RealityLens_Result";
    private TextView tvVerdict, tvConfidence, tvRealityScore, tvExplanation, tvEvidence;
    private ImageView ivResultImage;
    private ProgressBar progressBar;
    private String jobId;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private int retryCount = 0;
    
    private static final int MAX_RETRIES = 30; 
    private static final int POLL_INTERVAL = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_result);

        tvVerdict = findViewById(R.id.tv_verdict);
        ivResultImage = findViewById(R.id.iv_result_image);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvRealityScore = findViewById(R.id.tv_reality_score);
        tvExplanation = findViewById(R.id.tv_explanation);
        tvEvidence = findViewById(R.id.tv_evidence);
        progressBar = findViewById(R.id.loading_progress);
        Button btnBack = findViewById(R.id.btn_back_dashboard);

        jobId = getIntent().getStringExtra("job_id");

        if (jobId != null && !jobId.isEmpty()) {
            fetchResult();
        } else {
            Toast.makeText(this, "Job ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void fetchResult() {
        progressBar.setVisibility(View.VISIBLE);
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        String authHeader = "Bearer " + token;

        Log.d(TAG, "Requesting result for job: " + jobId + " (Attempt " + (retryCount + 1) + ")");

        RetrofitClient.getApiService().getResult(authHeader, jobId).enqueue(new Callback<ResultResponse>() {
            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response) {
                // Status 202 means the server is still processing the AI analysis
                if (response.code() == 202) {
                    Log.d(TAG, "Server status 202: Analysis still in progress...");
                    handleRetry();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "Result received successfully.");
                    progressBar.setVisibility(View.GONE);
                    displayData(response.body());
                } else {
                    Log.e(TAG, "Server error: " + response.code());
                    handleFailure("Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t) {
                Log.e(TAG, "Network or Parsing error: " + t.getMessage());
                handleFailure(t.getMessage());
            }
        });
    }

    private void handleRetry() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            pollHandler.postDelayed(this::fetchResult, POLL_INTERVAL);
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Analysis timed out. Please check later.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleFailure(String error) {
        // If it's a parsing error or temporary network glitch, try again
        if (retryCount < MAX_RETRIES) {
            handleRetry();
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load result: " + error, Toast.LENGTH_LONG).show();
        }
    }

    private void displayData(ResultResponse result) {
        tvVerdict.setText(result.getVerdict() != null ? result.getVerdict().toUpperCase() : "LIKELY REAL");
        
        // Formatted strings for numeric values
        tvConfidence.setText(result.getConfidence() != null ? (int)(result.getConfidence() * 100) + "%" : "N/A");
        tvRealityScore.setText(result.getRealityScore() != null ? result.getRealityScore() + "/1.0" : "N/A");
        
        tvExplanation.setText(result.getExplanation() != null ? result.getExplanation() : "No explanation available.");
        
        // Display Evidence items one by one
        List<ResultResponse.EvidenceItem> evidenceItems = result.getEvidence();
        if (evidenceItems != null && !evidenceItems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < evidenceItems.size(); i++) {
                ResultResponse.EvidenceItem item = evidenceItems.get(i);
                sb.append(i + 1).append(". ").append(item.getTitle()).append("\nSource: ").append(item.getSource());
                if (i < evidenceItems.size() - 1) sb.append("\n\n");
            }
            tvEvidence.setText(sb.toString());
        } else {
            tvEvidence.setText("No evidence found.");
        }

        if (result.getImageUrl() != null && !result.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(result.getImageUrl())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivResultImage);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacksAndMessages(null);
    }
}
