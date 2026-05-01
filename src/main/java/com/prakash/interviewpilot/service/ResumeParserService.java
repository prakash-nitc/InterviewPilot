package com.prakash.interviewpilot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Extracts text content from uploaded PDF resumes.
 *
 * This is part of the RESUME-AWARE INTERVIEWS feature.
 * The extracted text is passed to the AI prompt so it can generate
 * personalized questions based on the candidate's actual experience.
 *
 * WHY Apache PDFBox?
 * - Pure Java — no native dependencies, works everywhere.
 * - Well-maintained Apache project (production-grade).
 * - Simple API: load PDF → extract text in ~3 lines of code.
 * - Handles complex PDF layouts, multi-page documents, and various encodings.
 *
 * WHY truncate to 8000 chars?
 * - AI models have token limits (context window).
 * - A typical 2-page resume is ~3000-5000 chars of text.
 * - 8000 chars gives us headroom for longer resumes while staying safe.
 * - The resume text is appended to the question generation prompt,
 *   which itself is ~500 chars, so we need to leave room.
 */
@Service
public class ResumeParserService {

    private static final Logger log = LoggerFactory.getLogger(ResumeParserService.class);
    private static final int MAX_RESUME_LENGTH = 8000;

    /**
     * Extracts text from an uploaded PDF file.
     *
     * @param file The uploaded PDF file (MultipartFile from Spring MVC)
     * @return The extracted text content, truncated to MAX_RESUME_LENGTH
     * @throws IOException if the PDF cannot be read or parsed
     */
    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Resume file is empty or null");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new IllegalArgumentException("Only PDF files are supported. Received: " + contentType);
        }

        log.info("Extracting text from resume PDF: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Clean up the extracted text
            text = cleanResumeText(text);

            log.info("Extracted {} characters from resume (pages: {})",
                    text.length(), document.getNumberOfPages());

            // Truncate if too long
            if (text.length() > MAX_RESUME_LENGTH) {
                log.info("Resume text truncated from {} to {} characters", text.length(), MAX_RESUME_LENGTH);
                text = text.substring(0, MAX_RESUME_LENGTH);
            }

            return text;
        }
    }

    /**
     * Cleans up extracted PDF text.
     * PDF extraction often produces messy output with excessive whitespace,
     * page headers/footers, and other artifacts.
     */
    private String cleanResumeText(String text) {
        if (text == null) return "";

        // Normalize whitespace: collapse multiple spaces/newlines
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");

        // Remove common PDF artifacts
        text = text.replaceAll("\\f", ""); // form feed characters

        return text.trim();
    }
}
