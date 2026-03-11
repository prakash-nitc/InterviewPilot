package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.config.GeminiProperties;
import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QuestionGenerationService.
 *
 * We mock GeminiApiClient so we don't actually hit the Google API
 * during unit tests. This keeps tests fast and free.
 */
@ExtendWith(MockitoExtension.class)
class QuestionGenerationServiceTest {

    @Mock
    private GeminiApiClient geminiApiClient;

    @Mock
    private GeminiProperties geminiProperties;

    // Mocks for deep property fields
    @Mock
    private GeminiProperties.Questions questionsProperties;

    @InjectMocks
    private QuestionGenerationService questionGenerationService;

    @BeforeEach
    void setUp() {
        // Lenient because not every test method uses these stubs.
        // Without lenient(), Mockito's strict mode throws UnnecessaryStubbing.
        lenient().when(geminiProperties.getQuestions()).thenReturn(questionsProperties);
        lenient().when(questionsProperties.getCount()).thenReturn(3);
    }

    @Test
    @DisplayName("Generate questions - should return parsed list")
    void generateQuestions_shouldReturnParsedList() {
        // Arrange
        String fakeAiResponse = """
                Here are your questions:

                1. What is the difference between an Abstract Class and an Interface?
                2. Explain the concept of polymorphism.
                3. How does garbage collection work in Java?

                Good luck!
                """;

        when(geminiApiClient.generateContent(anyString())).thenReturn(fakeAiResponse);

        // Act
        List<String> questions = questionGenerationService.generateQuestions(
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.MEDIUM);

        // Assert
        assertEquals(3, questions.size());
        assertEquals("What is the difference between an Abstract Class and an Interface?", questions.get(0));
        assertEquals("Explain the concept of polymorphism.", questions.get(1));
        assertEquals("How does garbage collection work in Java?", questions.get(2));

        verify(geminiApiClient, times(1)).generateContent(anyString());
    }

    @Test
    @DisplayName("Parse questions - should ignore non-question text and numbering")
    void parseQuestions_shouldCleanAndFilter() {
        // Arrange
        String aiResponse = """
                Sure, here are some questions:

                1. First valid question?
                2) Second valid question with parens
                3- Third valid question with dash
                4.    Fourth valid question with spaces

                Just some extra text that doesn't matter.
                """;

        // Act (testing package-private parsing method directly)
        List<String> parsed = questionGenerationService.parseQuestions(aiResponse, 4);

        // Assert
        assertEquals(4, parsed.size());
        assertEquals("First valid question?", parsed.get(0));
        assertEquals("Second valid question with parens", parsed.get(1));
        assertEquals("Third valid question with dash", parsed.get(2));
        assertEquals("Fourth valid question with spaces", parsed.get(3));
    }

    @Test
    @DisplayName("Parse questions - should truncate if AI returns too many")
    void parseQuestions_shouldTruncateToExpectedCount() {
        String aiResponse = """
                1. Question one
                2. Question two
                3. Question three
                4. Question four
                """;

        // We only expect 2 questions
        List<String> parsed = questionGenerationService.parseQuestions(aiResponse, 2);

        assertEquals(2, parsed.size());
        assertEquals("Question one", parsed.get(0));
        assertEquals("Question two", parsed.get(1));
    }

    @Test
    @DisplayName("Build prompt - should include correct persona and parameters")
    void buildPrompt_shouldIncludeCorrectParameters() {
        // Act
        String prompt = questionGenerationService.buildPrompt(
                InterviewRole.DATA_SCIENTIST, InterviewTopic.PYTHON, Difficulty.HARD, 5);

        // Assert
        assertTrue(prompt.contains("Data Scientist"));
        assertTrue(prompt.contains("Python Programming"));
        assertTrue(prompt.contains("Hard"));
        assertTrue(prompt.contains("Generate exactly 5"));
        assertTrue(prompt.contains("numbered list"));
    }
}
