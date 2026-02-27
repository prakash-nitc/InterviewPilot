package com.prakash.interviewpilot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single mock interview session.
 *
 * Each session has a role (e.g., SDE), topic (e.g., DSA),
 * difficulty level, and a lifecycle status.
 *
 * WHY @Entity?
 * - Marks this class as a JPA entity — Hibernate maps it to a database table.
 * - Each instance = one row in the "interview_sessions" table.
 *
 * WHY @Table(name = "interview_sessions")?
 * - Explicitly names the table. Without it, JPA would use "InterviewSession"
 * which
 * isn't SQL convention (SQL uses snake_case).
 */
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    /**
     * WHY @GeneratedValue(strategy = GenerationType.IDENTITY)?
     * - Database auto-generates unique IDs (auto-increment).
     * - IDENTITY strategy delegates ID generation to the database, which is the
     * simplest and most portable approach for H2 and PostgreSQL.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * WHY @Enumerated(EnumType.STRING)?
     * - Stores the enum as a string ("SDE") instead of an ordinal (0).
     * - String storage is safer: if you reorder enum values, data doesn't break.
     * - Easier to read in the database and debug.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.NOT_STARTED;

    private int totalScore;

    private int maxScore;

    /**
     * WHY @OneToMany with mappedBy?
     * - One session has many questions.
     * - "mappedBy = session" means the Question entity owns the relationship
     * (it has the foreign key column).
     *
     * WHY CascadeType.ALL?
     * - When we save/delete a session, all its questions are saved/deleted too.
     *
     * WHY orphanRemoval = true?
     * - If a question is removed from the list, it's deleted from the database.
     */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<Question> questions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * WHY @PrePersist and @PreUpdate?
     * - JPA lifecycle callbacks — automatically set timestamps.
     * - @PrePersist runs before the first INSERT.
     * - @PreUpdate runs before every UPDATE.
     * - This avoids manually setting timestamps in service code.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Constructors ---

    public InterviewSession() {
        // Default constructor required by JPA
    }

    public InterviewSession(InterviewRole role, InterviewTopic topic, Difficulty difficulty) {
        this.role = role;
        this.topic = topic;
        this.difficulty = difficulty;
        this.status = SessionStatus.NOT_STARTED;
    }

    // --- Helper Methods ---

    /**
     * Adds a question to this session, maintaining the bidirectional relationship.
     * Always use this method instead of directly modifying the questions list.
     */
    public void addQuestion(Question question) {
        questions.add(question);
        question.setSession(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setSession(null);
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public InterviewRole getRole() {
        return role;
    }

    public void setRole(InterviewRole role) {
        this.role = role;
    }

    public InterviewTopic getTopic() {
        return topic;
    }

    public void setTopic(InterviewTopic topic) {
        this.topic = topic;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
