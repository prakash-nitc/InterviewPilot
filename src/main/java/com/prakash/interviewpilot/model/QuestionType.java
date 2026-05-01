package com.prakash.interviewpilot.model;

/**
 * Distinguishes between original interview questions and AI-generated follow-ups.
 *
 * WHY an enum instead of a boolean?
 * - Enums are extensible — if we later add BONUS or WARM_UP types, no schema change needed.
 * - More readable in the database: "FOLLOW_UP" vs "true".
 * - Follows the same pattern as our other enums (Difficulty, SessionStatus).
 *
 * HOW FOLLOW-UPS WORK:
 * - When the AI evaluates an answer and scores it below a threshold (e.g., ≤6/10),
 *   the system generates a probing follow-up question to dig deeper.
 * - This simulates real interviewer behavior: drilling down on weak spots.
 * - Follow-up questions have a parentQuestionId linking back to the original.
 */
public enum QuestionType {

    ORIGINAL("Original"),
    FOLLOW_UP("Follow-up");

    private final String displayName;

    QuestionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
