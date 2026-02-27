package com.prakash.interviewpilot.repository;

import com.prakash.interviewpilot.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Question entity.
 */
@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * Find all questions for a session, ordered by their position.
     */
    List<Question> findBySessionIdOrderByOrderIndexAsc(Long sessionId);

    /**
     * Find the first unanswered question in a session.
     * Used to determine which question to show next.
     */
    Question findFirstBySessionIdAndAnsweredFalseOrderByOrderIndexAsc(Long sessionId);

    /**
     * Count how many questions have been answered in a session.
     */
    long countBySessionIdAndAnsweredTrue(Long sessionId);
}
