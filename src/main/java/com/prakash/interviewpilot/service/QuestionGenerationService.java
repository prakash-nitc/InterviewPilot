package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.config.GeminiProperties;
import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates interview questions using the Gemini AI.
 *
 * This class handles:
 * 1. Building the prompt (prompt engineering)
 * 2. Calling the AI via GeminiApiClient
 * 3. Parsing the AI response into individual questions
 *
 * WHY separate from GeminiApiClient?
 * - GeminiApiClient is a generic HTTP client — reusable for any Gemini call.
 * - This service contains domain-specific prompt logic — what to ask the AI.
 * - In Phase 5, GeminiApiClient will be reused for answer evaluation,
 * but with a different prompt built by a different service.
 *
 * PROMPT ENGINEERING — why it matters:
 * - The quality of AI output depends heavily on how you ask.
 * - We use a structured prompt with clear instructions:
 * 1. Role assignment: "You are a technical interviewer"
 * 2. Context: role, topic, difficulty
 * 3. Format instructions: numbered list, no extra text
 * 4. Constraints: number of questions, difficulty calibration
 */
@Service
public class QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationService.class);

    private final GeminiApiClient geminiApiClient;
    private final GeminiProperties geminiProperties;

    public QuestionGenerationService(GeminiApiClient geminiApiClient, GeminiProperties geminiProperties) {
        this.geminiApiClient = geminiApiClient;
        this.geminiProperties = geminiProperties;
    }

    /**
     * Generates interview questions for the given role, topic, and difficulty.
     *
     * @return List of question strings
     */
    public List<String> generateQuestions(InterviewRole role, InterviewTopic topic, Difficulty difficulty) {
        int count = geminiProperties.getQuestions().getCount();

        String prompt = buildPrompt(role, topic, difficulty, count);
        log.info("Generating {} {} questions for {} - {}", count, difficulty, role, topic);

        String aiResponse = geminiApiClient.generateContent(prompt);

        List<String> questions = parseQuestions(aiResponse, count);
        log.info("Successfully parsed {} questions from AI response", questions.size());

        return questions;
    }

    /**
     * Builds the prompt for the AI.
     *
     * PROMPT DESIGN NOTES (for interviews):
     * - "Act as" sets the AI's persona — it responds more like an interviewer.
     * - Explicit format instructions ("numbered list") reduce ambiguity.
     * - "Do not include" prevents unwanted extras (greetings, explanations).
     * - Difficulty calibration ("entry-level" vs "staff engineer") guides
     * complexity.
     */
    String buildPrompt(InterviewRole role, InterviewTopic topic, Difficulty difficulty, int count) {
        String difficultyDescription = switch (difficulty) {
            case EASY -> "entry-level, suitable for freshers or junior developers with 0-1 years experience";
            case MEDIUM -> "intermediate, suitable for mid-level developers with 2-4 years experience";
            case HARD -> "advanced, suitable for senior/staff engineers with 5+ years experience";
        };

        return String.format("""
                You are an experienced technical interviewer conducting a mock interview.

                Role: %s
                Topic: %s
                Difficulty: %s (%s)

                Generate exactly %d interview questions for this mock interview session.

                Rules:
                1. Questions must be relevant to the role and topic.
                2. Questions should match the specified difficulty level.
                3. Include a mix of conceptual, analytical, and scenario-based questions.
                4. Format as a numbered list (1. 2. 3. etc.)
                5. Each question should be self-contained and clear.
                6. Do NOT include answers, hints, or any extra text.
                7. Do NOT include greetings, introductions, or closing remarks.
                8. Only output the numbered questions, nothing else.
                """,
                role.getDisplayName(),
                topic.getDisplayName(),
                difficulty.getDisplayName(),
                difficultyDescription,
                count);
    }

    /**
     * Parses the AI response into individual question strings.
     *
     * Expected format from the AI:
     * 1. What is polymorphism?
     * 2. Explain the difference between...
     * 3. How would you design...
     *
     * WHY regex-free parsing?
     * - Simple string operations are easier to understand and debug.
     * - We trim numbering prefixes ("1. ", "2. ") to get clean question text.
     * - If AI adds unexpected text (blank lines, headers), we filter them out.
     */
    List<String> parseQuestions(String aiResponse, int expectedCount) {
        List<String> questions = new ArrayList<>();

        String[] lines = aiResponse.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();

            // Skip empty lines
            if (trimmed.isEmpty()) {
                continue;
            }

            // Remove numbering prefix: "1. ", "2) ", "1- " etc.
            String cleaned = trimmed.replaceFirst("^\\d+[.)\\-]\\s*", "");

            // Skip if the line was only a number or is too short to be a question
            if (cleaned.length() < 10) {
                continue;
            }

            questions.add(cleaned);
        }

        // If we got fewer questions than expected, log a warning
        if (questions.size() < expectedCount) {
            log.warn("Expected {} questions but only parsed {}. AI response may have been truncated.",
                    expectedCount, questions.size());
        }

        // If we got more than expected, trim to the expected count
        if (questions.size() > expectedCount) {
            questions = questions.subList(0, expectedCount);
        }

        return questions;
    }
}
