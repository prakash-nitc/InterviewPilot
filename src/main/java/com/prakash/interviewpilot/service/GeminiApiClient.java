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
 * Low-level HTTP client for communicating with the AI API (Groq / OpenAI-compatible).
 *
 * WHY Groq instead of Gemini?
 * - Groq offers a generous free tier with fast inference.
 * - Uses the OpenAI-compatible API format, making it easy to switch providers.
 * - Bearer token authentication (industry standard).
 *
 * WHY @Retryable?
 * - External APIs can fail for temporary reasons (rate limit, network blip).
 * - @Retryable automatically retries the method up to maxAttempts times.
 * - backoff = @Backoff(delay = 1000, multiplier = 2):
 *   1st retry after 1s, 2nd after 2s, 3rd after 4s (exponential backoff).
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
     * Sends a prompt to the AI API and returns the generated text.
     *
     * @param prompt The text prompt to send
     * @return The AI-generated text response
     * @throws RestClientException if the API call fails after all retries
     */
    @Retryable(retryFor = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public String generateContent(String prompt) {
        log.info("Calling AI API (Groq)...");

        String url = geminiProperties.getApi().getUrl();
        String apiKey = geminiProperties.getApi().getKey();
        String model = geminiProperties.getApi().getModel();

        GeminiRequest request = new GeminiRequest(prompt, model);

        GeminiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        if (response == null || response.getGeneratedText() == null) {
            throw new RestClientException("AI API returned an empty response");
        }

        log.info("AI API response received successfully");
        return response.getGeneratedText();
    }
}
