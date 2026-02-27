package com.prakash.interviewpilot.model;

/**
 * Tracks the lifecycle state of an interview session.
 *
 * State transitions:
 * NOT_STARTED → IN_PROGRESS → COMPLETED
 */
public enum SessionStatus {

    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String displayName;

    SessionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
