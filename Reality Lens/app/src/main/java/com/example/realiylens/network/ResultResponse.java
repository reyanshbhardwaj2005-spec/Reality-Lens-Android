package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResultResponse {
    @SerializedName("job_id")
    private String jobId;

    @SerializedName("claim")
    private String claim;

    @SerializedName("verdict")
    private String verdict;

    @SerializedName("confidence")
    private Double confidence;

    @SerializedName("reality_score")
    private Double realityScore;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("evidence")
    private List<EvidenceItem> evidence;

    public String getJobId() { return jobId; }
    public String getClaim() { return claim; }
    public String getVerdict() { return verdict; }
    public Double getConfidence() { return confidence; }
    public Double getRealityScore() { return realityScore; }
    public String getExplanation() { return explanation; }
    public String getImageUrl() { return imageUrl; }
    public String getCreatedAt() { return createdAt; }
    public List<EvidenceItem> getEvidence() { return evidence; }

    public static class EvidenceItem {
        @SerializedName("url")
        private String url;
        @SerializedName("title")
        private String title;
        @SerializedName("source")
        private String source;
        @SerializedName("stance")
        private String stance;

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getSource() { return source; }
        public String getStance() { return stance; }
    }
}
