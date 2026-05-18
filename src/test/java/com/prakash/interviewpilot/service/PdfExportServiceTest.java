package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PdfExportService.
 *
 * WHY test PDF generation?
 * - Verifies the service doesn't throw on valid input.
 * - Ensures output is a valid PDF (starts with %PDF header).
 * - Catches encoding issues (special characters, non-ASCII).
 */
class PdfExportServiceTest {

    private final PdfExportService pdfExportService = new PdfExportService();

    @Test
    @DisplayName("Generate report - should produce valid PDF bytes for completed session")
    void generateReport_shouldProduceValidPdf() throws Exception {
        InterviewSession session = new InterviewSession(
                InterviewRole.SDE, InterviewTopic.DSA, Difficulty.MEDIUM);
        session.setStatus(SessionStatus.COMPLETED);
        session.setTotalScore(35);
        session.setMaxScore(50);

        Question q1 = new Question("What is a binary tree?", 1);
        q1.setAnswered(true);
        q1.setUserAnswer("A tree data structure where each node has at most two children.");
        q1.setScore(8);
        q1.setMaxScore(10);
        q1.setFeedback("Good answer, but could mention balanced vs unbalanced trees.");
        q1.setModelAnswer("A binary tree is a hierarchical data structure...");
        session.addQuestion(q1);

        Question q2 = new Question("Explain time complexity of quicksort.", 2);
        q2.setAnswered(true);
        q2.setUserAnswer("O(n log n) average, O(n^2) worst case.");
        q2.setScore(9);
        q2.setMaxScore(10);
        session.addQuestion(q2);

        byte[] pdf = pdfExportService.generateReport(session);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "PDF should be non-trivial size");
        // PDF files start with %PDF
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header, "Output should be a valid PDF file");
    }

    @Test
    @DisplayName("Generate report - should handle session with no answered questions")
    void generateReport_shouldHandleEmptySession() throws Exception {
        InterviewSession session = new InterviewSession(
                InterviewRole.DATA_SCIENTIST, InterviewTopic.PYTHON, Difficulty.EASY);
        session.setStatus(SessionStatus.NOT_STARTED);

        byte[] pdf = pdfExportService.generateReport(session);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }

    @Test
    @DisplayName("Generate report - should sanitize special characters without crashing")
    void generateReport_shouldHandleSpecialCharacters() throws Exception {
        InterviewSession session = new InterviewSession(
                InterviewRole.SDE, InterviewTopic.JAVA, Difficulty.HARD);
        session.setStatus(SessionStatus.COMPLETED);
        session.setTotalScore(7);
        session.setMaxScore(10);

        Question q = new Question("What\u2019s the difference between \u201Cabstract\u201D and \u201Cinterface\u201D?", 1);
        q.setAnswered(true);
        q.setUserAnswer("An abstract class can have state\u2026 an interface can\u2019t \u2014 mostly.");
        q.setScore(7);
        q.setMaxScore(10);
        q.setFeedback("Good \u2022 but mention default methods in Java 8+");
        session.addQuestion(q);

        byte[] pdf = pdfExportService.generateReport(session);

        assertNotNull(pdf);
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header);
    }
}
