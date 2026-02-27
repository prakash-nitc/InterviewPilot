package com.prakash.interviewpilot.repository;

import com.prakash.interviewpilot.model.InterviewSession;
import com.prakash.interviewpilot.model.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for InterviewSession entity.
 *
 * WHY extend JpaRepository?
 * - JpaRepository provides CRUD methods for free: save(), findById(),
 * findAll(), delete(), count(), etc.
 * - We only need to declare custom query methods — Spring Data JPA
 * generates the SQL automatically from the method name.
 *
 * WHY @Repository?
 * - Marks this as a Spring bean in the persistence layer.
 * - Also enables automatic exception translation (converts SQL exceptions
 * to Spring's DataAccessException hierarchy).
 *
 * HOW does method-name-based query work?
 * - findByStatus(status) → SELECT * FROM interview_sessions WHERE status = ?
 * - findAllByOrderByCreatedAtDesc() → SELECT * FROM interview_sessions ORDER BY
 * created_at DESC
 * - Spring parses the method name and builds the query. No SQL needed!
 */
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    /**
     * Find all sessions with a given status.
     * E.g., find all IN_PROGRESS sessions.
     */
    List<InterviewSession> findByStatus(SessionStatus status);

    /**
     * Find all sessions ordered by most recent first.
     * Used for the session history page.
     */
    List<InterviewSession> findAllByOrderByCreatedAtDesc();
}
