package com.prakash.interviewpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe configuration for Gemini API.
 *
 * WHY @ConfigurationProperties?
 * - Maps YAML/properties to a Java object automatically.
 * - Type-safe: you get compile-time checks instead of string-based property
 * lookups.
 * - Instead of using @Value("${gemini.api.key}") everywhere, inject this single
 * bean.
 *
 * Maps to application.yml:
 * gemini:
 * api:
 * key: ...
 * url: ...
 * questions:
 * count: 5
 */
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private Api api = new Api();
    private Questions questions = new Questions();

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Questions getQuestions() {
        return questions;
    }

    public void setQuestions(Questions questions) {
        this.questions = questions;
    }

    /**
     * Nested class for gemini.api.* properties.
     */
    public static class Api {
        private String key;
        private String url;
        private String model;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    /**
     * Nested class for gemini.questions.* properties.
     */
    public static class Questions {
        private int count = 5;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }
}
