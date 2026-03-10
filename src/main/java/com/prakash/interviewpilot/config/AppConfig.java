package com.prakash.interviewpilot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;

/**
 * Application-wide configuration.
 *
 * WHY @EnableRetry?
 * - Activates Spring Retry's @Retryable annotation support.
 * - Without this, @Retryable on service methods would be silently ignored.
 *
 * WHY RestClient instead of RestTemplate?
 * - RestClient is Spring Boot 3.2+'s modern HTTP client.
 * - Fluent API: readable chained method calls (like a builder).
 * - RestTemplate is legacy — still works but no new features.
 */
@Configuration
@EnableRetry
public class AppConfig {

    /**
     * Creates a shared RestClient bean for making HTTP calls.
     *
     * WHY a @Bean?
     * - Single instance shared across the app (singleton).
     * - Can be easily mocked in tests.
     * - Centralized configuration (timeouts, interceptors, etc.).
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
