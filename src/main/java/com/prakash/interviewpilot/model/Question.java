package com.prakash.interviewpilot.model;

import jakarta.persistence.*;
import com.prakash.interviewpilot.model.QuestionType;

/**
 * Represents a single interview question within a session.
 *
 * Each question belongs to one InterviewSession (ManyToOne relationship).
 * Questions are ordered by orderIndex within a session.
 *
 * WHY separate entity instead of embedded?
 * - Questions have their own lifecycle (scored independently).
 * - Need to query questions independently (e.g., "show unanswered questions").
 * - Each question stores its own score, feedback, and user answer.
 */
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WHY @ManyToOne(fetch = FetchType.LAZY)?
     * - Many questions belong to one session.
     * - LAZY loading: don't load the full session object every time we query a
     * question — only load it when we actually access session.getXxx().
     * - This is a performance optimization (avoids N+1 query problems).
     *
     * WHY @JoinColumn(name = "session_id")?
     * - This is the foreign key column in the "questions" table.
     * - Explicitly naming it makes the schema clear.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(nullable = false, length = 2000)
    private String questionText;

    @Column(length = 5000)
    private String userAnswer;

    private Integer score;

    private Integer maxScore;

    @Column(length = 5000)
    private String feedback;

    @Column(length = 5000)
    private String modelAnswer;

    /**
     * Determines the display order of questions within a session.
     * First question = 1, second = 2, etc.
     */
    @Column(nullable = false)
    private int orderIndex;

    private boolean answered = false;

    /**
     * Distinguishes between original questions and AI-generated follow-ups.
     * ORIGINAL = part of the initial question set generated at session start.
     * FOLLOW_UP = dynamically generated when the user gives a weak answer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType questionType = QuestionType.ORIGINAL;

    /**
     * For FOLLOW_UP questions, this links back to the original question
     * that triggered the follow-up. Null for ORIGINAL questions.
     * WHY Long instead of @ManyToOne? — Simpler schema, no circular references.
     */
    private Long parentQuestionId;

    // --- Constructors ---

    public Question() {
        // Default constructor required by JPA
    }

    public Question(String questionText, int orderIndex) {
        this.questionText = questionText;
        this.orderIndex = orderIndex;
        this.answered = false;
        this.questionType = QuestionType.ORIGINAL;
    }

    /**
     * Constructor for follow-up questions.
     * Allows specifying the QuestionType explicitly.
     */
    public Question(String questionText, int orderIndex, QuestionType questionType) {
        this.questionText = questionText;
        this.orderIndex = orderIndex;
        this.answered = false;
        this.questionType = questionType;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewSession getSession() {
        return session;
    }

    public void setSession(InterviewSession session) {
        this.session = session;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Long getParentQuestionId() {
        return parentQuestionId;
    }

    public void setParentQuestionId(Long parentQuestionId) {
        this.parentQuestionId = parentQuestionId;
    }
}
