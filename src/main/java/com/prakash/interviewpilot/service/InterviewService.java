package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.InterviewSession;
import com.prakash.interviewpilot.model.Question;
import com.prakash.interviewpilot.model.SessionStatus;
import com.prakash.interviewpilot.repository.InterviewSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for managing interview sessions.
 *
 * WHY @Service?
 * - Marks this as a Spring-managed bean in the service layer.
 * - Spring will create a single instance (singleton) and inject it wherever
 * needed.
 * - Functionally similar to @Component, but communicates intent — this is
 * business logic.
 *
 * WHY a Service layer at all?
 * - Controllers should be "thin" — they handle HTTP, not business logic.
 * - Service layer encapsulates all business rules (validation, state
 * transitions, etc.).
 * - Easier to test — we can test InterviewService without any HTTP/web context.
 * - Reusable — same service can be used by REST controllers, WebSocket
 * handlers, etc.
 *
 * WHY Constructor Injection (not @Autowired on fields)?
 * - Makes dependencies explicit and required (fail-fast if missing).
 * - The class can be instantiated in unit tests without Spring (just pass
 * mocks).
 * - Fields can be final → immutable after construction → thread-safe.
 * - Spring team officially recommends constructor injection.
 */
@Service
@Transactional
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final InterviewSessionRepository sessionRepository;
    private final QuestionGenerationService questionGenerationService;

    /**
     * Constructor injection — Spring automatically injects both dependencies.
     * Since there's only one constructor, @Autowired is optional (Spring 4.3+).
     */
    public InterviewService(InterviewSessionRepository sessionRepository,
            QuestionGenerationService questionGenerationService) {
        this.sessionRepository = sessionRepository;
        this.questionGenerationService = questionGenerationService;
    }

    /**
     * Creates a new interview session from the form data.
     * Session starts in NOT_STARTED status.
     */
    public InterviewSession createSession(CreateSessionRequest request) {
        InterviewSession session = new InterviewSession(
                request.getRole(),
                request.getTopic(),
                request.getDifficulty());
        return sessionRepository.save(session);
    }

    /**
     * Retrieves a session by ID.
     * Throws an exception if not found — we'll handle this gracefully in the
     * controller.
     *
     * WHY orElseThrow() instead of returning null?
     * - Fail-fast: calling code doesn't need to check for null.
     * - Clear error message helps with debugging.
     * - Follows Optional best practices.
     */
    @Transactional(readOnly = true)
    public InterviewSession getSession(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview session not found with id: " + id));
    }

    /**
     * Returns all sessions, most recent first.
     */
    @Transactional(readOnly = true)
    public List<InterviewSession> getAllSessions() {
        return sessionRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Returns all sessions with the given status.
     */
    @Transactional(readOnly = true)
    public List<InterviewSession> getSessionsByStatus(SessionStatus status) {
        return sessionRepository.findByStatus(status);
    }

    /**
     * Transitions a session from NOT_STARTED to IN_PROGRESS.
     * Generates AI interview questions and saves them to the session.
     *
     * WHY generate questions at start time (not at creation time)?
     * - Session creation is instant (good UX for the form).
     * - The AI call takes 2-5 seconds — we delay it to "Start" button.
     * - If the AI fails, the session still exists and can be retried.
     */
    public InterviewSession startSession(Long id) {
        InterviewSession session = getSession(id);

        if (session.getStatus() != SessionStatus.NOT_STARTED) {
            throw new IllegalStateException(
                    "Cannot start session — current status is " + session.getStatus());
        }

        // Generate questions using AI
        log.info("Starting session {} — generating AI questions", id);
        List<String> questionTexts = questionGenerationService.generateQuestions(
                session.getRole(),
                session.getTopic(),
                session.getDifficulty());

        // Create Question entities and attach to session
        for (int i = 0; i < questionTexts.size(); i++) {
            Question question = new Question(questionTexts.get(i), i + 1);
            question.setMaxScore(10); // Default max score per question
            session.addQuestion(question);
        }

        session.setMaxScore(questionTexts.size() * 10);
        session.setStatus(SessionStatus.IN_PROGRESS);

        return sessionRepository.save(session);
    }

    /**
     * Marks a session as completed and calculates the final score.
     * Called after the last question is answered.
     */
    public InterviewSession completeSession(Long id) {
        InterviewSession session = getSession(id);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Cannot complete session — current status is " + session.getStatus());
        }

        // Calculate total score from all questions
        int totalScore = session.getQuestions().stream()
                .filter(q -> q.getScore() != null)
                .mapToInt(q -> q.getScore())
                .sum();

        int maxScore = session.getQuestions().stream()
                .filter(q -> q.getMaxScore() != null)
                .mapToInt(q -> q.getMaxScore())
                .sum();

        session.setTotalScore(totalScore);
        session.setMaxScore(maxScore);
        session.setStatus(SessionStatus.COMPLETED);

        return sessionRepository.save(session);
    }

    /**
     * Deletes a session and all its questions (due to CascadeType.ALL).
     */
    public void deleteSession(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new RuntimeException("Interview session not found with id: " + id);
        }
        sessionRepository.deleteById(id);
    }

    /**
     * Returns the total count of sessions.
     */
    @Transactional(readOnly = true)
    public long getSessionCount() {
        return sessionRepository.count();
    }
}
