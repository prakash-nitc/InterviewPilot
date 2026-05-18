package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.model.InterviewSession;
import com.prakash.interviewpilot.model.Question;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates a PDF report for a completed interview session.
 *
 * WHY PDFBox (not iText)?
 * - PDFBox is Apache-licensed (fully free), iText has AGPL restrictions.
 * - We already have PDFBox in our dependencies (for resume parsing).
 * - Simple text-based reports don't need iText's advanced layout engine.
 *
 * WHY generate in-memory (ByteArrayOutputStream)?
 * - No temp files to clean up.
 * - Can stream directly to HTTP response.
 * - Memory-efficient for small reports (< 100KB).
 */
@Service
public class PdfExportService {

    private static final Logger log = LoggerFactory.getLogger(PdfExportService.class);

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 15;
    private static final float HEADING_HEIGHT = 22;

    /**
     * Generates a PDF report for the given interview session.
     *
     * @return byte array containing the PDF file contents
     */
    public byte[] generateReport(InterviewSession session) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            addReportContent(document, session);
            document.save(out);

            log.info("Generated PDF report for session {} ({} bytes)", session.getId(), out.size());
            return out.toByteArray();
        }
    }

    private void addReportContent(PDDocument document, InterviewSession session) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        float pageWidth = page.getMediaBox().getWidth();
        float yPos = page.getMediaBox().getHeight() - MARGIN;

        PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        PDPageContentStream content = new PDPageContentStream(document, page);

        // Title
        yPos = drawText(content, "InterviewPilot - Session Report", fontBold, 18, MARGIN, yPos, pageWidth);
        yPos -= 10;

        // Divider
        yPos = drawLine(content, MARGIN, yPos, pageWidth - MARGIN, yPos);
        yPos -= 15;

        // Session Info
        yPos = drawText(content, "Role: " + session.getRole().getDisplayName(), fontRegular, 11, MARGIN, yPos, pageWidth);
        yPos = drawText(content, "Topic: " + session.getTopic().getDisplayName(), fontRegular, 11, MARGIN, yPos, pageWidth);
        yPos = drawText(content, "Difficulty: " + session.getDifficulty().getDisplayName(), fontRegular, 11, MARGIN, yPos, pageWidth);
        yPos = drawText(content, "Status: " + session.getStatus().getDisplayName(), fontRegular, 11, MARGIN, yPos, pageWidth);

        if (session.getMaxScore() > 0) {
            int percent = session.getTotalScore() * 100 / session.getMaxScore();
            yPos = drawText(content, "Score: " + session.getTotalScore() + "/" + session.getMaxScore()
                    + " (" + percent + "%)", fontBold, 11, MARGIN, yPos, pageWidth);
        }

        yPos -= 20;
        yPos = drawText(content, "Questions & Answers", fontBold, 14, MARGIN, yPos, pageWidth);
        yPos -= 5;
        yPos = drawLine(content, MARGIN, yPos, pageWidth - MARGIN, yPos);
        yPos -= 15;

        // Questions
        float contentWidth = pageWidth - (2 * MARGIN);
        for (Question q : session.getQuestions()) {
            // Check if we need a new page
            if (yPos < 150) {
                content.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                content = new PDPageContentStream(document, page);
                yPos = page.getMediaBox().getHeight() - MARGIN;
            }

            // Question header
            yPos = drawText(content, "Q" + q.getOrderIndex() + ": " + truncate(q.getQuestionText(), 80),
                    fontBold, 10, MARGIN, yPos, pageWidth);

            if (q.isAnswered() && q.getUserAnswer() != null) {
                yPos = drawWrappedText(content, "Answer: " + q.getUserAnswer(),
                        fontRegular, 9, MARGIN + 10, yPos, contentWidth - 10);
            }

            if (q.getScore() != null) {
                yPos = drawText(content, "Score: " + q.getScore() + "/" + q.getMaxScore(),
                        fontBold, 9, MARGIN + 10, yPos, pageWidth);
            }

            if (q.getFeedback() != null) {
                yPos = drawWrappedText(content, "Feedback: " + q.getFeedback(),
                        fontItalic, 9, MARGIN + 10, yPos, contentWidth - 10);
            }

            if (q.getModelAnswer() != null) {
                yPos = drawWrappedText(content, "Model Answer: " + q.getModelAnswer(),
                        fontRegular, 8, MARGIN + 10, yPos, contentWidth - 10);
            }

            yPos -= 12;
        }

        // Footer
        if (yPos < 80) {
            content.close();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            yPos = page.getMediaBox().getHeight() - MARGIN;
        }
        yPos -= 10;
        yPos = drawLine(content, MARGIN, yPos, pageWidth - MARGIN, yPos);
        yPos -= 15;
        drawText(content, "Generated by InterviewPilot | AI-Powered Mock Interview Platform",
                fontItalic, 8, MARGIN, yPos, pageWidth);

        content.close();
    }

    private float drawText(PDPageContentStream content, String text, PDType1Font font,
                           float fontSize, float x, float y, float pageWidth) throws IOException {
        // Sanitize text — PDFBox can't render certain characters
        text = sanitize(text);
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - LINE_HEIGHT;
    }

    private float drawWrappedText(PDPageContentStream content, String text, PDType1Font font,
                                  float fontSize, float x, float y, float maxWidth) throws IOException {
        text = sanitize(text);
        // Simple word-wrap
        float spaceWidth = font.getStringWidth(" ") / 1000 * fontSize;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        float lineWidth = 0;

        for (String word : words) {
            float wordWidth = font.getStringWidth(word) / 1000 * fontSize;
            if (lineWidth + wordWidth > maxWidth && line.length() > 0) {
                // Flush line
                content.beginText();
                content.setFont(font, fontSize);
                content.newLineAtOffset(x, y);
                content.showText(line.toString().trim());
                content.endText();
                y -= LINE_HEIGHT;
                line = new StringBuilder();
                lineWidth = 0;
            }
            line.append(word).append(" ");
            lineWidth += wordWidth + spaceWidth;
        }

        // Flush remaining
        if (line.length() > 0) {
            content.beginText();
            content.setFont(font, fontSize);
            content.newLineAtOffset(x, y);
            content.showText(line.toString().trim());
            content.endText();
            y -= LINE_HEIGHT;
        }

        return y;
    }

    private float drawLine(PDPageContentStream content, float x1, float y1,
                           float x2, float y2) throws IOException {
        content.setLineWidth(0.5f);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
        return y1 - 5;
    }

    /**
     * Sanitize text for PDFBox — replace characters that can't be encoded in WinAnsiEncoding.
     */
    private String sanitize(String text) {
        if (text == null) return "";
        return text
                .replace("\u2018", "'").replace("\u2019", "'")
                .replace("\u201C", "\"").replace("\u201D", "\"")
                .replace("\u2014", "--").replace("\u2013", "-")
                .replace("\u2026", "...")
                .replace("\u2022", "*")
                .replaceAll("[^\\x00-\\x7F]", "");  // Remove any remaining non-ASCII
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        text = sanitize(text);
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
