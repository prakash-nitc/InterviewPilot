package com.prakash.interviewpilot.dto;

import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewTopic;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for the "Start Interview" form submission.
 *
 * WHY use a DTO instead of the entity directly?
 * - Separation of concerns: the form only needs 3 fields, but the entity has
 * many more.
 * - Validation: we validate the DTO before creating the entity.
 * - Security: prevents users from injecting unexpected fields (like id or
 * status).
 * - Clean API: the controller receives exactly what it needs, nothing more.
 *
 * WHY @NotNull?
 * - Part of Jakarta Bean Validation (JSR 380).
 * - Spring automatically validates DTOs annotated with @Valid in the
 * controller.
 * - If validation fails, Spring returns errors without our code even running.
 */
public class CreateSessionRequest {

    @NotNull(message = "Please select a role")
    private InterviewRole role;

    @NotNull(message = "Please select a topic")
    private InterviewTopic topic;

    @NotNull(message = "Please select a difficulty")
    private Difficulty difficulty;

    // --- Constructors ---

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(InterviewRole role, InterviewTopic topic, Difficulty difficulty) {
        this.role = role;
        this.topic = topic;
        this.difficulty = difficulty;
    }

    // --- Getters and Setters ---

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
}
