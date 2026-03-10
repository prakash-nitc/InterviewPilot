package com.prakash.interviewpilot.dto;

import java.util.List;

/**
 * Represents the request body sent to the Gemini API.
 *
 * Gemini expects this JSON format:
 * {
 * "contents": [
 * { "parts": [ { "text": "your prompt here" } ] }
 * ]
 * }
 *
 * WHY nested classes?
 * - Matches the exact JSON structure the API expects.
 * - Jackson (Spring's JSON library) serializes these objects directly to JSON.
 * - No manual JSON string building needed.
 */
public class GeminiRequest {

    private List<Content> contents;

    public GeminiRequest() {
    }

    public GeminiRequest(String prompt) {
        this.contents = List.of(new Content(List.of(new Part(prompt))));
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {
        private List<Part> parts;

        public Content() {
        }

        public Content(List<Part> parts) {
            this.parts = parts;
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {
        private String text;

        public Part() {
        }

        public Part(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
