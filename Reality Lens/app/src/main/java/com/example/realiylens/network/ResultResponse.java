package com.example.realiylens.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ResultResponse {
    @SerializedName("verdict")
    private String verdict;

    @SerializedName("evidence")
    private List<EvidenceItem> evidence;

    @SerializedName("confidence")
    private Double confidence;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("reality_score")
    private Double realityScore;

    @SerializedName("image_url")
    private String imageUrl;

    public String getVerdict() { return verdict; }
    public List<EvidenceItem> getEvidence() { return evidence; }
    public Double getConfidence() { return confidence; }
    public String getExplanation() { return explanation; }
    public Double getRealityScore() { return realityScore; }
    public String getImageUrl() { return imageUrl; }

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
