package com.prakash.interviewpilot.dto;

/**
 * Holds aggregated statistics for the dashboard.
 *
 * WHY a DTO instead of multiple model attributes?
 * - Groups related stats into a single object — cleaner controller code.
 * - Easy to extend with new metrics without changing method signatures.
 * - Testable: we can verify all stats are computed correctly as a unit.
 */
public class DashboardStats {

    private long totalSessions;
    private long completedSessions;
    private long inProgressSessions;
    private double averageScorePercent;
    private int totalQuestionsAnswered;
    private String bestTopic;
    private String bestGrade;

    public DashboardStats() {
    }

    // --- Getters and Setters ---

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getCompletedSessions() {
        return completedSessions;
    }

    public void setCompletedSessions(long completedSessions) {
        this.completedSessions = completedSessions;
    }

    public long getInProgressSessions() {
        return inProgressSessions;
    }

    public void setInProgressSessions(long inProgressSessions) {
        this.inProgressSessions = inProgressSessions;
    }

    public double getAverageScorePercent() {
        return averageScorePercent;
    }

    public void setAverageScorePercent(double averageScorePercent) {
        this.averageScorePercent = averageScorePercent;
    }

    public int getTotalQuestionsAnswered() {
        return totalQuestionsAnswered;
    }

    public void setTotalQuestionsAnswered(int totalQuestionsAnswered) {
        this.totalQuestionsAnswered = totalQuestionsAnswered;
    }

    public String getBestTopic() {
        return bestTopic;
    }

    public void setBestTopic(String bestTopic) {
        this.bestTopic = bestTopic;
    }

    public String getBestGrade() {
        return bestGrade;
    }

    public void setBestGrade(String bestGrade) {
        this.bestGrade = bestGrade;
    }

    /**
     * Computes a letter grade from the average score percentage.
     */
    public static String computeGrade(double percent) {
        if (percent >= 90) return "A+";
        if (percent >= 80) return "A";
        if (percent >= 70) return "B";
        if (percent >= 60) return "C";
        if (percent >= 50) return "D";
        return "F";
    }
}
