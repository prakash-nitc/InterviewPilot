package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.InterviewSession;
import com.prakash.interviewpilot.model.SessionStatus;
import com.prakash.interviewpilot.repository.InterviewSessionRepository;
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

    private final InterviewSessionRepository sessionRepository;

    /**
     * Constructor injection — Spring automatically injects the repository.
     * Since there's only one constructor, @Autowired is optional (Spring 4.3+).
     */
    public InterviewService(InterviewSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
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
     * This is called when the first question is presented to the user.
     */
    public InterviewSession startSession(Long id) {
        InterviewSession session = getSession(id);

        if (session.getStatus() != SessionStatus.NOT_STARTED) {
            throw new IllegalStateException(
                    "Cannot start session — current status is " + session.getStatus());
        }

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
