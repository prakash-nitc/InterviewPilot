package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.*;
import com.prakash.interviewpilot.repository.InterviewSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InterviewService.
 *
 * WHY @ExtendWith(MockitoExtension.class)?
 * - Initializes @Mock and @InjectMocks annotations automatically.
 * - No Spring context needed — these are fast, isolated unit tests.
 *
 * WHY mock the repository?
 * - We want to test the SERVICE LOGIC, not the database.
 * - Mocking the repository lets us control what data it returns.
 * - Tests run in milliseconds instead of seconds (no DB startup).
 */
@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewSessionRepository sessionRepository;

    @Mock
    private QuestionGenerationService questionGenerationService;

    @InjectMocks
    private InterviewService interviewService;

    private CreateSessionRequest validRequest;
    private InterviewSession savedSession;

    @BeforeEach
    void setUp() {
        validRequest = new CreateSessionRequest(
                InterviewRole.SDE,
                InterviewTopic.DSA,
                Difficulty.MEDIUM);

        savedSession = new InterviewSession(
                InterviewRole.SDE,
                InterviewTopic.DSA,
                Difficulty.MEDIUM);
        savedSession.setId(1L);
    }

    @Test
    @DisplayName("Create session - should save and return new session")
    void createSession_shouldSaveAndReturnSession() {
        // Arrange
        when(sessionRepository.save(any(InterviewSession.class))).thenReturn(savedSession);

        // Act
        InterviewSession result = interviewService.createSession(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(InterviewRole.SDE, result.getRole());
        assertEquals(InterviewTopic.DSA, result.getTopic());
        assertEquals(Difficulty.MEDIUM, result.getDifficulty());
        assertEquals(SessionStatus.NOT_STARTED, result.getStatus());

        // Verify repository was called exactly once
        verify(sessionRepository, times(1)).save(any(InterviewSession.class));
    }

    @Test
    @DisplayName("Get session by ID - should return session when found")
    void getSession_shouldReturnSessionWhenFound() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        InterviewSession result = interviewService.getSession(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Get session by ID - should throw exception when not found")
    void getSession_shouldThrowWhenNotFound() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> interviewService.getSession(999L));

        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("Get all sessions - should return sessions ordered by date")
    void getAllSessions_shouldReturnOrderedList() {
        InterviewSession session1 = new InterviewSession(InterviewRole.SDE, InterviewTopic.DSA, Difficulty.EASY);
        InterviewSession session2 = new InterviewSession(InterviewRole.DATA_SCIENTIST, InterviewTopic.PYTHON,
                Difficulty.HARD);

        when(sessionRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(Arrays.asList(session2, session1));

        List<InterviewSession> result = interviewService.getAllSessions();

        assertEquals(2, result.size());
        verify(sessionRepository, times(1)).findAllByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Start session - should generate questions and transition to IN_PROGRESS")
    void startSession_shouldTransitionToInProgress() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(sessionRepository.save(any(InterviewSession.class))).thenReturn(savedSession);

        // Mock AI generating 3 questions
        when(questionGenerationService.generateQuestions(
                any(InterviewRole.class), any(InterviewTopic.class), any(Difficulty.class)))
                .thenReturn(List.of("Question 1?", "Question 2?", "Question 3?"));

        InterviewSession result = interviewService.startSession(1L);

        assertEquals(SessionStatus.IN_PROGRESS, result.getStatus());
        assertEquals(3, result.getQuestions().size());
        verify(questionGenerationService, times(1)).generateQuestions(
                any(InterviewRole.class), any(InterviewTopic.class), any(Difficulty.class));
    }

    @Test
    @DisplayName("Start session - should throw if session already in progress")
    void startSession_shouldThrowIfAlreadyInProgress() {
        savedSession.setStatus(SessionStatus.IN_PROGRESS);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.startSession(1L));
    }

    @Test
    @DisplayName("Complete session - should calculate score and mark completed")
    void completeSession_shouldCalculateScoreAndComplete() {
        savedSession.setStatus(SessionStatus.IN_PROGRESS);

        // Add some scored questions
        Question q1 = new Question("Question 1", 1);
        q1.setScore(7);
        q1.setMaxScore(10);
        q1.setAnswered(true);

        Question q2 = new Question("Question 2", 2);
        q2.setScore(9);
        q2.setMaxScore(10);
        q2.setAnswered(true);

        savedSession.addQuestion(q1);
        savedSession.addQuestion(q2);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(sessionRepository.save(any(InterviewSession.class))).thenReturn(savedSession);

        InterviewSession result = interviewService.completeSession(1L);

        assertEquals(SessionStatus.COMPLETED, result.getStatus());
        assertEquals(16, result.getTotalScore());
        assertEquals(20, result.getMaxScore());
    }

    @Test
    @DisplayName("Complete session - should throw if session not in progress")
    void completeSession_shouldThrowIfNotInProgress() {
        // Session is NOT_STARTED, can't complete
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.completeSession(1L));
    }

    @Test
    @DisplayName("Delete session - should delete when session exists")
    void deleteSession_shouldDeleteWhenExists() {
        when(sessionRepository.existsById(1L)).thenReturn(true);

        interviewService.deleteSession(1L);

        verify(sessionRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Delete session - should throw when session not found")
    void deleteSession_shouldThrowWhenNotFound() {
        when(sessionRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                RuntimeException.class,
                () -> interviewService.deleteSession(999L));
    }

    @Test
    @DisplayName("Get current question - should return first unanswered question")
    void getCurrentQuestion_shouldReturnFirstUnanswered() {
        Question q1 = new Question("Q1", 1);
        q1.setId(10L);
        q1.setAnswered(true);

        Question q2 = new Question("Q2", 2);
        q2.setId(11L);
        q2.setAnswered(false);

        Question q3 = new Question("Q3", 3);
        q3.setId(12L);
        q3.setAnswered(false);

        savedSession.addQuestion(q1);
        savedSession.addQuestion(q2);
        savedSession.addQuestion(q3);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        Question result = interviewService.getCurrentQuestion(1L);

        assertNotNull(result);
        assertEquals("Q2", result.getQuestionText());
    }

    @Test
    @DisplayName("Get current question - should return null when all answered")
    void getCurrentQuestion_shouldReturnNullWhenAllAnswered() {
        Question q1 = new Question("Q1", 1);
        q1.setAnswered(true);
        savedSession.addQuestion(q1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        Question result = interviewService.getCurrentQuestion(1L);

        assertNull(result);
    }

    @Test
    @DisplayName("Submit answer - should mark question as answered")
    void submitAnswer_shouldMarkQuestionAsAnswered() {
        Question q1 = new Question("Q1", 1);
        q1.setId(10L);
        q1.setAnswered(false);
        savedSession.addQuestion(q1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(sessionRepository.save(any(InterviewSession.class))).thenReturn(savedSession);

        Question result = interviewService.submitAnswer(1L, 10L, "My answer here");

        assertTrue(result.isAnswered());
        assertEquals("My answer here", result.getUserAnswer());
    }

    @Test
    @DisplayName("Submit answer - should throw if question already answered")
    void submitAnswer_shouldThrowIfAlreadyAnswered() {
        Question q1 = new Question("Q1", 1);
        q1.setId(10L);
        q1.setAnswered(true);
        savedSession.addQuestion(q1);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));

        assertThrows(
                IllegalStateException.class,
                () -> interviewService.submitAnswer(1L, 10L, "Another answer"));
    }
}
