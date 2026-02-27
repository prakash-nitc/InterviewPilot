package com.prakash.interviewpilot.model;

/**
 * Represents the difficulty level of the interview.
 * Affects the complexity of AI-generated questions.
 */
public enum Difficulty {

    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
