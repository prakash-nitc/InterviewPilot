package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.dto.EvaluationResult;
import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AnswerEvaluationService.
 *
 * We mock GeminiApiClient so we don't actually hit the Groq API
 * during unit tests. This keeps tests fast and free.
 */
@ExtendWith(MockitoExtension.class)
class AnswerEvaluationServiceTest {

    @Mock
    private GeminiApiClient geminiApiClient;

    @InjectMocks
    private AnswerEvaluationService answerEvaluationService;

    @Test
    @DisplayName("Evaluate answer - should parse valid JSON response")
    void evaluateAnswer_shouldParseValidJsonResponse() {
        // Arrange: AI returns clean JSON
        String aiResponse = """
                {"score": 8, "maxScore": 10, "feedback": "Good understanding of polymorphism.", "modelAnswer": "Polymorphism allows objects to take many forms."}
                """;

        when(geminiApiClient.generateContent(anyString())).thenReturn(aiResponse);

        // Act
        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "What is polymorphism?",
                "Polymorphism is when objects can behave differently based on their type.",
                InterviewRole.SDE,
                InterviewTopic.JAVA,
                Difficulty.EASY);

        // Assert
        assertEquals(8, result.getScore());
        assertEquals(10, result.getMaxScore());
        assertEquals("Good understanding of polymorphism.", result.getFeedback());
        assertEquals("Polymorphism allows objects to take many forms.", result.getModelAnswer());

        verify(geminiApiClient, times(1)).generateContent(anyString());
    }

    @Test
    @DisplayName("Evaluate answer - should handle JSON wrapped in markdown code fences")
    void evaluateAnswer_shouldHandleMarkdownFences() {
        String aiResponse = """
                ```json
                {"score": 6, "maxScore": 10, "feedback": "Partial understanding.", "modelAnswer": "The correct answer is..."}
                ```
                """;

        when(geminiApiClient.generateContent(anyString())).thenReturn(aiResponse);

        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "What is OOP?", "Objects and classes.",
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.EASY);

        assertEquals(6, result.getScore());
        assertEquals("Partial understanding.", result.getFeedback());
    }

    @Test
    @DisplayName("Evaluate answer - should handle JSON with preamble text")
    void evaluateAnswer_shouldHandlePreambleText() {
        String aiResponse = """
                Here's my evaluation:
                {"score": 9, "maxScore": 10, "feedback": "Excellent answer!", "modelAnswer": "Perfect."}
                """;

        when(geminiApiClient.generateContent(anyString())).thenReturn(aiResponse);

        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "What is Java?", "Java is a platform-independent language.",
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.EASY);

        assertEquals(9, result.getScore());
        assertEquals("Excellent answer!", result.getFeedback());
    }

    @Test
    @DisplayName("Evaluate answer - should return fallback on malformed response")
    void evaluateAnswer_shouldReturnFallbackOnMalformedResponse() {
        // AI returns garbage instead of JSON
        when(geminiApiClient.generateContent(anyString())).thenReturn("I can't evaluate this.");

        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "What is Java?", "I don't know.",
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.EASY);

        // Should get fallback result
        assertEquals(5, result.getScore());
        assertEquals(10, result.getMaxScore());
        assertNotNull(result.getFeedback());
    }

    @Test
    @DisplayName("Evaluate answer - should return fallback when API throws exception")
    void evaluateAnswer_shouldReturnFallbackOnApiFailure() {
        when(geminiApiClient.generateContent(anyString()))
                .thenThrow(new RuntimeException("API error"));

        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "What is Java?", "I don't know.",
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.EASY);

        assertEquals(5, result.getScore());
        assertEquals(10, result.getMaxScore());
    }

    @Test
    @DisplayName("Evaluate answer - should clamp score to valid range")
    void evaluateAnswer_shouldClampScoreToValidRange() {
        // AI returns score > maxScore
        String aiResponse = """
                {"score": 15, "maxScore": 10, "feedback": "Perfect!", "modelAnswer": "Yes."}
                """;

        when(geminiApiClient.generateContent(anyString())).thenReturn(aiResponse);

        EvaluationResult result = answerEvaluationService.evaluateAnswer(
                "Q?", "A.",
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.EASY);

        assertEquals(10, result.getScore()); // Clamped to maxScore
    }

    @Test
    @DisplayName("Build prompt - should include question, answer, and context")
    void buildPrompt_shouldIncludeAllContext() {
        String prompt = answerEvaluationService.buildEvaluationPrompt(
                "What is polymorphism?",
                "It allows objects to behave differently.",
                InterviewRole.SDE,
                InterviewTopic.JAVA,
                Difficulty.MEDIUM);

        assertTrue(prompt.contains("What is polymorphism?"));
        assertTrue(prompt.contains("It allows objects to behave differently."));
        assertTrue(prompt.contains("Software Development Engineer"));
        assertTrue(prompt.contains("Java"));
        assertTrue(prompt.contains("Medium"));
        assertTrue(prompt.contains("JSON"));
    }
}
