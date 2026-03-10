package com.prakash.interviewpilot.dto;

import java.util.List;

/**
 * Represents the response body from the Gemini API.
 *
 * Gemini returns this JSON structure:
 * {
 * "candidates": [
 * {
 * "content": {
 * "parts": [ { "text": "generated text here" } ]
 * }
 * }
 * ]
 * }
 *
 * WHY do we only model the fields we need?
 * - Jackson ignores unknown JSON properties by default.
 * - The real API response has many more fields (safetyRatings, usageMetadata,
 * etc.)
 * but we only care about the generated text.
 */
public class GeminiResponse {

    private List<Candidate> candidates;

    public GeminiResponse() {
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    /**
     * Convenience method to extract the generated text from the response.
     * Returns null if the response structure is unexpected.
     */
    public String getGeneratedText() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.getContent() != null
                    && candidate.getContent().getParts() != null
                    && !candidate.getContent().getParts().isEmpty()) {
                return candidate.getContent().getParts().get(0).getText();
            }
        }
        return null;
    }

    public static class Candidate {
        private Content content;

        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }
    }

    public static class Content {
        private List<Part> parts;

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
