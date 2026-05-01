package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.config.GeminiProperties;
import com.prakash.interviewpilot.dto.EvaluationResult;
import com.prakash.interviewpilot.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Generates adaptive follow-up questions based on the user's answer quality.
 *
 * This is the NOVEL FEATURE that differentiates InterviewPilot from a simple ChatGPT prompt.
 *
 * HOW IT WORKS:
 * 1. After the user answers a question, the AI evaluates their answer (score 0-10).
 * 2. If the score is below a configurable threshold (default: 6), this service
 *    generates a probing follow-up question.
 * 3. The follow-up is inserted dynamically into the session's question list.
 * 4. The user must answer the follow-up before proceeding to the next original question.
 *
 * WHY THIS IS NOVEL:
 * - ChatGPT/Gemini gems can't enforce this structured flow.
 * - It simulates real interviewer behavior: drilling down on weak answers.
 * - It requires AI chaining: answer evaluation → conditional follow-up generation.
 * - It demonstrates a state machine with AI-driven transitions.
 *
 * WHY a separate service?
 * - Single Responsibility: follow-up logic is distinct from answer evaluation.
 * - Testable: we can mock this service to test InterviewService independently.
 * - Configurable: follow-ups can be enabled/disabled via application.yml.
 */
@Service
public class FollowUpService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

    private final GeminiApiClient geminiApiClient;
    private final GeminiProperties geminiProperties;

    public FollowUpService(GeminiApiClient geminiApiClient, GeminiProperties geminiProperties) {
        this.geminiApiClient = geminiApiClient;
        this.geminiProperties = geminiProperties;
    }

    /**
     * Decides whether a follow-up is needed and generates one if so.
     *
     * DECISION LOGIC:
     * - If follow-ups are disabled in config → return null.
     * - If the answered question is already a follow-up → return null (no chain of follow-ups).
     * - If the evaluation score is above the threshold → return null.
     * - Otherwise → generate and return a follow-up Question entity.
     *
     * @param session           The current interview session (for context)
     * @param answeredQuestion  The question that was just answered
     * @param evaluation        The AI's evaluation of the user's answer
     * @return A new Question entity of type FOLLOW_UP, or null if no follow-up needed
     */
    public Question generateFollowUpIfNeeded(
            InterviewSession session,
            Question answeredQuestion,
            EvaluationResult evaluation) {

        // Check if follow-ups are enabled
        if (!geminiProperties.getFollowup().isEnabled()) {
            log.debug("Follow-up questions are disabled in configuration");
            return null;
        }

        // Don't generate follow-ups for follow-up questions (prevent infinite chains)
        if (answeredQuestion.getQuestionType() == QuestionType.FOLLOW_UP) {
            log.debug("Skipping follow-up generation — answered question is already a follow-up");
            return null;
        }

        // Check if score is above threshold (good answer → no follow-up needed)
        int threshold = geminiProperties.getFollowup().getScoreThreshold();
        if (evaluation.getScore() > threshold) {
            log.debug("Score {}/{} is above threshold {} — no follow-up needed",
                    evaluation.getScore(), evaluation.getMaxScore(), threshold);
            return null;
        }

        // Count existing follow-ups for this question
        int maxFollowUps = geminiProperties.getFollowup().getMaxPerQuestion();
        long existingFollowUps = session.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.FOLLOW_UP)
                .filter(q -> answeredQuestion.getId().equals(q.getParentQuestionId()))
                .count();

        if (existingFollowUps >= maxFollowUps) {
            log.debug("Max follow-ups ({}) already reached for question {}", maxFollowUps, answeredQuestion.getId());
            return null;
        }

        // Generate the follow-up question
        log.info("Score {}/{} is below threshold {} — generating follow-up question",
                evaluation.getScore(), evaluation.getMaxScore(), threshold);

        try {
            String prompt = buildFollowUpPrompt(
                    answeredQuestion.getQuestionText(),
                    answeredQuestion.getUserAnswer(),
                    evaluation.getFeedback(),
                    session.getRole(),
                    session.getTopic(),
                    session.getDifficulty());

            String followUpText = geminiApiClient.generateContent(prompt);

            // Clean up the response (remove numbering, extra whitespace)
            followUpText = cleanFollowUpText(followUpText);

            // Create the follow-up Question entity
            // orderIndex is set to parent's index (it will be re-indexed by the caller)
            Question followUp = new Question(followUpText, answeredQuestion.getOrderIndex(), QuestionType.FOLLOW_UP);
            followUp.setParentQuestionId(answeredQuestion.getId());
            followUp.setMaxScore(10);

            log.info("Follow-up question generated successfully: {}...",
                    followUpText.substring(0, Math.min(60, followUpText.length())));

            return followUp;

        } catch (Exception e) {
            log.error("Failed to generate follow-up question, continuing without it", e);
            return null;
        }
    }

    /**
     * Builds the prompt for generating a follow-up question.
     *
     * PROMPT DESIGN:
     * - We provide the original question, the user's weak answer, and the AI's feedback.
     * - We instruct the AI to ask a SPECIFIC probing question that digs deeper
     *   into the weak area identified in the feedback.
     * - We ask for exactly ONE question (no numbering, no extras).
     */
    String buildFollowUpPrompt(
            String questionText,
            String userAnswer,
            String feedback,
            InterviewRole role,
            InterviewTopic topic,
            Difficulty difficulty) {

        return String.format("""
                You are an experienced technical interviewer. The candidate gave a weak answer \
                to a question, and you need to ask a follow-up to probe deeper.

                Interview Context:
                - Role: %s
                - Topic: %s
                - Difficulty: %s

                Original Question: %s

                Candidate's Answer: %s

                Your Feedback on their answer: %s

                Based on the weakness identified in the feedback, generate exactly ONE \
                follow-up question that:
                1. Probes deeper into the specific concept the candidate struggled with
                2. Is more focused and specific than the original question
                3. Helps assess if the candidate has a fundamental misunderstanding
                4. Is appropriate for the difficulty level

                Rules:
                - Output ONLY the follow-up question text, nothing else.
                - Do NOT include numbering, prefixes, or labels.
                - Do NOT include "Follow-up:" or similar prefixes.
                - Do NOT include explanations or context.
                - The question should be 1-2 sentences maximum.
                """,
                role.getDisplayName(),
                topic.getDisplayName(),
                difficulty.getDisplayName(),
                questionText,
                userAnswer,
                feedback);
    }

    /**
     * Cleans up the AI's response to get just the question text.
     * Handles cases where the AI adds prefixes, numbering, or extra whitespace.
     */
    private String cleanFollowUpText(String text) {
        if (text == null || text.isBlank()) {
            return "Can you elaborate on your previous answer?";
        }

        String cleaned = text.trim();

        // Remove common prefixes the AI might add
        cleaned = cleaned.replaceFirst("(?i)^(follow-up:|follow up:|question:|q:)\\s*", "");

        // Remove numbering prefix (1. or 1) etc.)
        cleaned = cleaned.replaceFirst("^\\d+[.)\\-]\\s*", "");

        // Remove surrounding quotes
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }

        return cleaned.trim();
    }
}
