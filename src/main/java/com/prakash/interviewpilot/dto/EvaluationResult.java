package com.prakash.interviewpilot.dto;

/**
 * Holds the parsed result of an AI answer evaluation.
 *
 * WHY a separate DTO instead of setting fields directly?
 * - Clean separation: the parsing logic returns a structured object.
 * - Testable: we can verify parsing output without touching the Question entity.
 * - Reusable: if we ever need to evaluate answers outside of a session context.
 *
 * The AI returns JSON like:
 * {
 *   "score": 7,
 *   "maxScore": 10,
 *   "feedback": "Good understanding of the concept...",
 *   "modelAnswer": "Polymorphism is..."
 * }
 */
public class EvaluationResult {

    private int score;
    private int maxScore;
    private String feedback;
    private String modelAnswer;

    public EvaluationResult() {
    }

    public EvaluationResult(int score, int maxScore, String feedback, String modelAnswer) {
        this.score = score;
        this.maxScore = maxScore;
        this.feedback = feedback;
        this.modelAnswer = modelAnswer;
    }

    // --- Getters and Setters ---

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getModelAnswer() {
        return modelAnswer;
    }

    public void setModelAnswer(String modelAnswer) {
        this.modelAnswer = modelAnswer;
    }
}
