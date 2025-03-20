package org.sunbird.cert.Models;

public class AssessmentUserAttempt {

    private String contentId;
    private double score;
    private double totalScore;

    public AssessmentUserAttempt(String contentId, double score, double totalScore) {
        this.contentId = contentId;
        this.score = score;
        this.totalScore = totalScore;
    }

    // Getters
    public String getContentId() { return contentId; }
    public double getScore() { return score; }
    public double getTotalScore() { return totalScore; }
}
