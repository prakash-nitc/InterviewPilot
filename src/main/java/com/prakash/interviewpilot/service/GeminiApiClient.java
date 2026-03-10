package com.prakash.interviewpilot.service;

import com.prakash.interviewpilot.config.GeminiProperties;
import com.prakash.interviewpilot.dto.GeminiRequest;
import com.prakash.interviewpilot.dto.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Low-level HTTP client for communicating with Google's Gemini API.
 *
 * This class ONLY handles:
 * - Building the HTTP request
 * - Sending it to Gemini
 * - Returning the raw response
 *
 * It does NOT handle:
 * - Prompt engineering (that's QuestionGenerationService's job)
 * - Parsing questions from the text (also QuestionGenerationService)
 *
 * WHY separate this from QuestionGenerationService?
 * - Single Responsibility: one class for HTTP, one for prompt logic.
 * - Testability: we can mock this class easily in tests.
 * - Reusability: if we later need Gemini for answer evaluation (Phase 5),
 * we reuse this same client.
 *
 * WHY @Retryable?
 * - External APIs can fail for temporary reasons (rate limit, network blip).
 * - @Retryable automatically retries the method up to maxAttempts times.
 * - backoff = @Backoff(delay = 1000, multiplier = 2):
 * 1st retry after 1s, 2nd after 2s, 3rd after 4s (exponential backoff).
 * - This avoids hammering the API and gives it time to recover.
 */
@Service
public class GeminiApiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiApiClient.class);

    private final RestClient restClient;
    private final GeminiProperties geminiProperties;

    public GeminiApiClient(RestClient restClient, GeminiProperties geminiProperties) {
        this.restClient = restClient;
        this.geminiProperties = geminiProperties;
    }

    /**
     * Sends a prompt to the Gemini API and returns the generated text.
     *
     * @param prompt The text prompt to send
     * @return The AI-generated text response
     * @throws RestClientException if the API call fails after all retries
     */
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String generateContent(String prompt) {
        log.info("Calling Gemini API...");

        String url = geminiProperties.getApi().getUrl() + "?key=" + geminiProperties.getApi().getKey();

        GeminiRequest request = new GeminiRequest(prompt);

        GeminiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null || response.getGeneratedText() == null) {
            throw new RestClientException("Gemini API returned an empty response");
        }

        log.info("Gemini API response received successfully");
        return response.getGeneratedText();
    }
}
