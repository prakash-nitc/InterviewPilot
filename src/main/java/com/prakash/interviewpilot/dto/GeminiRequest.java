package com.prakash.interviewpilot.dto;

import java.util.List;

/**
 * Represents the request body sent to the Groq API (OpenAI-compatible format).
 *
 * Groq expects this JSON format:
 * {
 *   "model": "llama-3.3-70b-versatile",
 *   "messages": [
 *     { "role": "user", "content": "your prompt here" }
 *   ]
 * }
 *
 * WHY OpenAI-compatible format?
 * - Groq implements the OpenAI chat completions API standard.
 * - This means we can easily switch between Groq, OpenAI, or any
 *   OpenAI-compatible provider by just changing the URL and key.
 */
public class GeminiRequest {

    private String model;
    private List<Message> messages;

    public GeminiRequest() {
    }

    public GeminiRequest(String prompt, String model) {
        this.model = model;
        this.messages = List.of(new Message("user", prompt));
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {
        private String role;
        private String content;

        public Message() {
        }

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

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
