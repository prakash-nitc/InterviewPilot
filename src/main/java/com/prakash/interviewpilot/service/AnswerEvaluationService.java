package com.prakash.interviewpilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prakash.interviewpilot.dto.EvaluationResult;
import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Evaluates user answers using AI and returns scores + feedback.
 *
 * This class handles:
 * 1. Building the evaluation prompt (prompt engineering)
 * 2. Calling the AI via GeminiApiClient
 * 3. Parsing the structured JSON response into an EvaluationResult
 *
 * WHY separate from QuestionGenerationService?
 * - Single Responsibility: question generation and answer evaluation
 *   are two distinct domain operations with different prompts.
 * - Both reuse GeminiApiClient for the actual HTTP call.
 * - This makes each service focused, testable, and easy to modify.
 *
 * PROMPT ENGINEERING for evaluation:
 * - We ask the AI to return structured JSON (not free-form text).
 * - JSON is easier to parse reliably than natural language.
 * - We provide a scoring rubric so the AI scores consistently.
 * - We include the interview context (role, topic, difficulty) so
 *   the AI calibrates expectations appropriately.
 */
@Service
public class AnswerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(AnswerEvaluationService.class);

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    public AnswerEvaluationService(GeminiApiClient geminiApiClient) {
        this.geminiApiClient = geminiApiClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Evaluates a user's answer against the question.
     *
     * @param questionText The interview question
     * @param userAnswer   The user's submitted answer
     * @param role         The interview role (for context)
     * @param topic        The interview topic (for context)
     * @param difficulty   The difficulty level (calibrates expectations)
     * @return EvaluationResult with score, feedback, and model answer
     */
    public EvaluationResult evaluateAnswer(
            String questionText,
            String userAnswer,
            InterviewRole role,
            InterviewTopic topic,
            Difficulty difficulty) {

        String prompt = buildEvaluationPrompt(questionText, userAnswer, role, topic, difficulty);
        log.info("Evaluating answer for question: {}...", questionText.substring(0, Math.min(50, questionText.length())));

        try {
            String aiResponse = geminiApiClient.generateContent(prompt);
            EvaluationResult result = parseEvaluation(aiResponse);
            log.info("Evaluation complete — score: {}/{}", result.getScore(), result.getMaxScore());
            return result;
        } catch (Exception e) {
            log.error("Answer evaluation failed, using default score", e);
            return createFallbackResult();
        }
    }

    /**
     * Builds the evaluation prompt.
     *
     * PROMPT DESIGN NOTES:
     * - "Act as" persona sets consistent behavior.
     * - We provide the full context: role, topic, difficulty, question, answer.
     * - We specify a scoring rubric (0-3, 4-6, 7-8, 9-10) for consistency.
     * - We demand JSON output with specific field names — easier to parse.
     * - "ONLY output valid JSON" prevents the AI from adding natural language.
     */
    String buildEvaluationPrompt(
            String questionText,
            String userAnswer,
            InterviewRole role,
            InterviewTopic topic,
            Difficulty difficulty) {

        return String.format("""
                You are an experienced technical interviewer evaluating a candidate's answer.

                Interview Context:
                - Role: %s
                - Topic: %s
                - Difficulty: %s

                Question: %s

                Candidate's Answer: %s

                Evaluate the answer using this rubric:
                - 0-3: Poor — Incorrect, irrelevant, or shows no understanding
                - 4-5: Below Average — Partially correct but missing key concepts
                - 6-7: Average — Correct basics but lacks depth or has minor errors
                - 8: Good — Mostly correct with good understanding, minor gaps
                - 9-10: Excellent — Comprehensive, accurate, well-structured answer

                Respond with ONLY valid JSON in this exact format (no markdown, no code fences, no extra text):
                {"score": <0-10>, "maxScore": 10, "feedback": "<2-3 sentences: what was good, what was missing, how to improve>", "modelAnswer": "<a concise ideal answer in 2-4 sentences>"}
                """,
                role.getDisplayName(),
                topic.getDisplayName(),
                difficulty.getDisplayName(),
                questionText,
                userAnswer);
    }

    /**
     * Parses the AI's JSON response into an EvaluationResult.
     *
     * WHY not just use objectMapper.readValue()?
     * - The AI sometimes wraps JSON in markdown code fences (```json ... ```).
     * - Sometimes it adds a preamble like "Here's the evaluation:".
     * - We need to extract just the JSON part and handle edge cases.
     *
     * PARSING STRATEGY:
     * 1. Try to find JSON between { and } (handles extra text around it).
     * 2. Use Jackson to parse the extracted JSON.
     * 3. If anything fails, return a fallback result instead of crashing.
     */
    EvaluationResult parseEvaluation(String aiResponse) {
        try {
            // Extract JSON from the response (handle markdown fences, extra text)
            String json = extractJson(aiResponse);

            JsonNode root = objectMapper.readTree(json);

            int score = root.has("score") ? root.get("score").asInt() : 5;
            int maxScore = root.has("maxScore") ? root.get("maxScore").asInt() : 10;
            String feedback = root.has("feedback") ? root.get("feedback").asText() : "No feedback available.";
            String modelAnswer = root.has("modelAnswer") ? root.get("modelAnswer").asText() : "No model answer available.";

            // Clamp score to valid range
            score = Math.max(0, Math.min(score, maxScore));

            return new EvaluationResult(score, maxScore, feedback, modelAnswer);

        } catch (Exception e) {
            log.warn("Failed to parse AI evaluation response: {}", e.getMessage());
            log.debug("Raw AI response: {}", aiResponse);
            return createFallbackResult();
        }
    }

    /**
     * Extracts a JSON object from a string that may contain extra text.
     *
     * Handles cases like:
     * - Pure JSON: {"score": 7, ...}
     * - Markdown fenced: ```json\n{"score": 7, ...}\n```
     * - Preamble text: "Here's my evaluation:\n{"score": 7, ...}"
     */
    private String extractJson(String text) {
        // Remove markdown code fences if present
        text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

        // Find the first { and last } to extract the JSON object
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start == -1 || end == -1 || end <= start) {
            throw new RuntimeException("No JSON object found in AI response");
        }

        return text.substring(start, end + 1);
    }

    /**
     * Creates a fallback result when AI evaluation fails.
     * Gives a neutral score so the interview can continue.
     */
    private EvaluationResult createFallbackResult() {
        return new EvaluationResult(
                5, 10,
                "Evaluation could not be completed automatically. Your answer has been recorded for manual review.",
                "Model answer not available.");
    }
}
