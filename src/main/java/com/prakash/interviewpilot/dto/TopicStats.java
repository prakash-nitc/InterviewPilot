package com.prakash.interviewpilot.dto;

/**
 * Holds performance statistics for a single interview topic.
 *
 * WHY a separate DTO instead of adding fields to DashboardStats?
 * - Each topic has its own stats (sessions, score, grade).
 * - The controller passes a List<TopicStats> — clean and iterable in Thymeleaf.
 * - Single Responsibility: DashboardStats = global, TopicStats = per-topic.
 */
public class TopicStats {

    private String topicName;
    private String topicEnumName;
    private long sessionCount;
    private int totalScore;
    private int maxScore;
    private double scorePercent;
    private String grade;

    public TopicStats() {
    }

    public TopicStats(String topicName, String topicEnumName, long sessionCount,
                      int totalScore, int maxScore) {
        this.topicName = topicName;
        this.topicEnumName = topicEnumName;
        this.sessionCount = sessionCount;
        this.totalScore = totalScore;
        this.maxScore = maxScore;
        this.scorePercent = maxScore > 0 ? Math.round(totalScore * 1000.0 / maxScore) / 10.0 : 0;
        this.grade = DashboardStats.computeGrade(this.scorePercent);
    }

    // --- Getters ---

    public String getTopicName() {
        return topicName;
    }

    public String getTopicEnumName() {
        return topicEnumName;
    }

    public long getSessionCount() {
        return sessionCount;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public double getScorePercent() {
        return scorePercent;
    }

    public String getGrade() {
        return grade;
    }
}
