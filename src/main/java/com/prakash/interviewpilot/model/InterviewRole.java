package com.prakash.interviewpilot.model;

/**
 * Represents the target job role for the mock interview.
 * Each role determines the type of questions the AI will generate.
 */
public enum InterviewRole {

    SDE("Software Development Engineer"),
    DATA_SCIENTIST("Data Scientist"),
    BACKEND_DEVELOPER("Backend Developer"),
    FRONTEND_DEVELOPER("Frontend Developer"),
    DEVOPS_ENGINEER("DevOps Engineer"),
    FULL_STACK_DEVELOPER("Full Stack Developer");

    private final String displayName;

    InterviewRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
