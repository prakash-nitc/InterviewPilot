package com.prakash.interviewpilot.dto;

import java.util.List;

/**
 * Represents the response body from the Groq API (OpenAI-compatible format).
 *
 * Groq returns this JSON structure:
 * {
 *   "choices": [
 *     {
 *       "message": {
 *         "role": "assistant",
 *         "content": "generated text here"
 *       }
 *     }
 *   ]
 * }
 *
 * WHY do we only model the fields we need?
 * - Jackson ignores unknown JSON properties by default.
 * - The real API response has many more fields (usage, id, model, etc.)
 *   but we only care about the generated text.
 */
public class GeminiResponse {

    private List<Choice> choices;

    public GeminiResponse() {
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    /**
     * Convenience method to extract the generated text from the response.
     * Returns null if the response structure is unexpected.
     */
    public String getGeneratedText() {
        if (choices != null && !choices.isEmpty()) {
            Choice choice = choices.get(0);
            if (choice.getMessage() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }

    public static class Choice {
        private Message message;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    public static class Message {
        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
