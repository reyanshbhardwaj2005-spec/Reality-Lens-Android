package com.example.realiylens;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.realiylens.network.ResultResponse;
import com.example.realiylens.network.RetrofitClient;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerificationResultActivity extends AppCompatActivity {

    private TextView tvVerdict, tvConfidence, tvRealityScore, tvExplanation;
    private LinearLayout llEvidenceContainer;
    private ImageView ivResultImage;
    private LinearProgressIndicator progressBar;
    private String jobId;
    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private int retryCount = 0;
    
    private static final int MAX_RETRIES = 45; 
    private static final int POLL_INTERVAL = 4000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_result);

        // Initialize views
        tvVerdict = findViewById(R.id.tv_verdict);
        ivResultImage = findViewById(R.id.iv_result_image);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvRealityScore = findViewById(R.id.tv_reality_score);
        tvExplanation = findViewById(R.id.tv_explanation);
        llEvidenceContainer = findViewById(R.id.ll_evidence_container);
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
        progressBar.show();
        
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String token = prefs.getString("access_token", "");
        String authHeader = "Bearer " + token;

        RetrofitClient.getApiService().getResult(authHeader, jobId).enqueue(new Callback<ResultResponse>() {
            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response) {
                if (response.code() == 202) {
                    handleRetry();
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    progressBar.hide();
                    displayData(response.body());
                } else {
                    handleFailure("Server Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t) {
                handleFailure(t.getMessage());
            }
        });
    }

    private void handleRetry() {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            pollHandler.postDelayed(this::fetchResult, POLL_INTERVAL);
        } else {
            progressBar.hide();
            Toast.makeText(this, "Analysis timed out. Please check later.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleFailure(String error) {
        if (retryCount < MAX_RETRIES) {
            handleRetry();
        } else {
            progressBar.hide();
            Toast.makeText(this, "Failed to load result", Toast.LENGTH_LONG).show();
        }
    }

    private void displayData(ResultResponse result) {
        tvVerdict.setText(result.getVerdict() != null ? result.getVerdict().toUpperCase() : "ANALYSIS COMPLETE");
        
        if (result.getConfidence() != null) {
            tvConfidence.setText((int)(result.getConfidence() * 100) + "%");
        }
        
        if (result.getRealityScore() != null) {
            tvRealityScore.setText(result.getRealityScore() + "/1.0");
        }
        
        tvExplanation.setText(result.getExplanation() != null ? result.getExplanation() : "No explanation available.");
        
        llEvidenceContainer.removeAllViews();
        List<ResultResponse.EvidenceItem> evidenceItems = result.getEvidence();
        if (evidenceItems != null && !evidenceItems.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(this);
            for (ResultResponse.EvidenceItem item : evidenceItems) {
                View itemView = inflater.inflate(R.layout.item_evidence, llEvidenceContainer, false);
                
                TextView tvTitle = itemView.findViewById(R.id.tv_evidence_title);
                TextView tvSource = itemView.findViewById(R.id.tv_evidence_source);
                
                tvTitle.setText(item.getTitle());
                tvSource.setText("Source: " + item.getSource());

                itemView.setOnClickListener(v -> {
                    if (item.getUrl() != null && !item.getUrl().isEmpty()) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getUrl()));
                        startActivity(browserIntent);
                    } else {
                        Toast.makeText(this, "Link not available", Toast.LENGTH_SHORT).show();
                    }
                });

                llEvidenceContainer.addView(itemView);
            }
        } else {
            TextView tvEmpty = new TextView(this);
            tvEmpty.setText("No evidence found.");
            tvEmpty.setTextColor(getResources().getColor(R.color.white));
            llEvidenceContainer.addView(tvEmpty);
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
