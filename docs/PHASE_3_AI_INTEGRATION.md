# Phase 3: AI Integration — Question Generation

> **Status:** ✅ Complete
> **Goal:** Integrate Google Gemini API to generate interview questions dynamically.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Architecture & Design Decisions](#architecture--design-decisions)
3. [Gemini API Integration](#gemini-api-integration)
4. [Prompt Engineering](#prompt-engineering)
5. [Configuration Management](#configuration-management)
6. [Retry & Resilience](#retry--resilience)
7. [Testing Strategy](#testing-strategy)
8. [Interview Q&A](#interview-qa)

---

## What Was Built

- **GeminiProperties** — Type-safe `@ConfigurationProperties` for API key, URL, question count
- **AppConfig** — `@EnableRetry` + `RestClient` bean
- **GeminiRequest / GeminiResponse DTOs** — Match Gemini's JSON API format
- **GeminiApiClient** — Low-level HTTP client with `@Retryable` (3 attempts, exponential backoff)
- **QuestionGenerationService** — Prompt engineering + AI response parsing
- **Updated InterviewService** — `startSession()` now generates AI questions
- **Updated InterviewController** — `POST /interviews/{id}/start` endpoint
- **4 new unit tests** for prompt building, parsing, and integration

---

## Architecture & Design Decisions

### Why Two Services Instead of One?
```
GeminiApiClient            QuestionGenerationService
  (HTTP concerns)            (Domain logic)
  - Build HTTP request       - Build the prompt
  - Send to Gemini API       - Call GeminiApiClient
  - Handle retries           - Parse AI response into questions
  - Return raw text          - Return List<String>
```

**Single Responsibility Principle:** Each service has one reason to change.
- If the Gemini API format changes → only `GeminiApiClient` changes.
- If we tweak the prompt → only `QuestionGenerationService` changes.
- In Phase 5, `GeminiApiClient` will be reused for answer evaluation with a different prompt.

### Why Generate Questions at Session Start (Not Creation)?
- **Session creation** is instant → fast form submission → good UX.
- **AI call takes 2-5 seconds** → delayed to the "Begin Interview" button click.
- **If AI fails**, the session still exists and user can retry.

---

## Gemini API Integration

### Request Format
```json
{
  "contents": [
    { "parts": [ { "text": "your prompt here" } ] }
  ]
}
```

### Response Format
```json
{
  "candidates": [
    {
      "content": {
        "parts": [ { "text": "AI generated text" } ]
      }
    }
  ]
}
```

### RestClient (Modern Spring HTTP Client)
```java
GeminiResponse response = restClient.post()
        .uri(url)
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)         // Jackson auto-serializes to JSON
        .retrieve()
        .body(GeminiResponse.class);  // Jackson auto-deserializes
```

**Why RestClient over RestTemplate?**
| RestClient (Spring 3.2+) | RestTemplate (Legacy) |
|---|---|
| Fluent/chainable API | Verbose method calls |
| Modern, actively developed | Maintenance mode |
| Consistent with WebClient | Different API style |

---

## Prompt Engineering

```java
String prompt = String.format("""
    You are an experienced technical interviewer...
    Role: %s
    Topic: %s
    Difficulty: %s (%s)
    Generate exactly %d questions...
    Rules:
    1. Format as a numbered list
    2. Do NOT include answers or extra text
    """, role, topic, difficulty, description, count);
```

### Key Prompt Design Principles
| Principle | Example | Why |
|---|---|---|
| **Persona Assignment** | "You are a technical interviewer" | AI adopts the role, gives more relevant output |
| **Explicit Format** | "numbered list (1. 2. 3.)" | Predictable output format → easier parsing |
| **Negative Instructions** | "Do NOT include answers" | Prevents unwanted content |
| **Difficulty Calibration** | "entry-level, 0-1 years" | Guides question complexity |
| **Constraint Specification** | "Generate exactly 5" | Controls output volume |

### Response Parsing
```java
// Only accept lines starting with a number prefix: "1.", "2)", "3-"
if (!trimmed.matches("^\\d+[.)\\-]\\s*.*")) continue;

// Strip the prefix to get clean question text
String cleaned = trimmed.replaceFirst("^\\d+[.)\\-]\\s*", "");
```

---

## Configuration Management

### @ConfigurationProperties vs @Value
```java
// BAD: Scattered @Value annotations
@Value("${gemini.api.key}") private String apiKey;
@Value("${gemini.api.url}") private String apiUrl;

// GOOD: Type-safe config class
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private Api api;  // gemini.api.key, gemini.api.url
    private Questions questions;  // gemini.questions.count
}
```

| @ConfigurationProperties | @Value |
|---|---|
| Type-safe, compile-time checks | String-based, fails at runtime |
| Grouped related properties | Scattered across classes |
| IDE autocomplete support | No autocomplete |
| **Recommended for production** | OK for simple values |

### Securing the API Key
```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY:your-api-key-here}  # Reads from env variable
```
- Key is **never committed to Git** — it's read from environment variable.
- The `your-api-key-here` default is a fallback for dev (won't work, but won't crash).

---

## Retry & Resilience

```java
@Retryable(
    retryFor = RestClientException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public String generateContent(String prompt) { ... }
```

### How Exponential Backoff Works
```
Attempt 1: Fails → wait 1 second
Attempt 2: Fails → wait 2 seconds (1000 * 2)
Attempt 3: Fails → throw exception (3 attempts exhausted)
```

**Why exponential backoff?**
- Gives the external service time to recover.
- Avoids "thundering herd" — many clients retrying at the same time.
- Industry standard for API integrations.

### Requirements for Spring Retry
1. `spring-retry` dependency in pom.xml
2. `spring-aspects` dependency (for AOP proxy)
3. `@EnableRetry` on a `@Configuration` class
4. `@Retryable` on the method to retry

---

## Testing Strategy

### What We Mock and Why
```java
@Mock private GeminiApiClient geminiApiClient;  // Don't hit real API
@Mock private GeminiProperties geminiProperties;  // Control config values

// Simulate AI response
when(geminiApiClient.generateContent(anyString()))
    .thenReturn("1. Question one?\n2. Question two?");
```

### Lenient Stubs
```java
// Not every test uses these stubs, so Mockito's strict mode complains.
lenient().when(geminiProperties.getQuestions()).thenReturn(questionsProperties);
```
- `lenient()` tells Mockito: "Don't fail if this stub isn't used in some tests."
- Needed when `@BeforeEach` sets up stubs shared across tests with different needs.

---

## Interview Q&A

### Q: How did you integrate the AI API?
**A:** I created a `GeminiApiClient` using Spring's RestClient to make HTTP POST calls to Google's Gemini API. The client sends a JSON request with a text prompt and deserializes the JSON response into a Java DTO using Jackson. I added `@Retryable` with exponential backoff to handle transient failures.

### Q: Explain your prompt engineering approach.
**A:** I use a structured prompt with five key elements: (1) persona assignment — "You are a technical interviewer," (2) context injection — role, topic, difficulty, (3) format specification — numbered list, (4) negative instructions — "do NOT include answers," and (5) difficulty calibration — mapping EASY/MEDIUM/HARD to experience levels. This produces consistent, parseable output.

### Q: Why did you separate GeminiApiClient from QuestionGenerationService?
**A:** Single Responsibility Principle. GeminiApiClient handles HTTP — it doesn't know what a "question" is. QuestionGenerationService handles domain logic — building prompts, parsing responses. This separation means GeminiApiClient can be reused in Phase 5 for answer evaluation with a completely different prompt.

### Q: What is @Retryable and why use it?
**A:** It's a Spring Retry annotation that automatically retries a method when a specified exception occurs. I configured 3 attempts with exponential backoff (1s → 2s → 4s). This handles transient issues like network timeouts or API rate limits without manual retry loops. It requires `spring-retry`, `spring-aspects`, and `@EnableRetry`.

### Q: How do you manage secrets like API keys?
**A:** The API key is read from an environment variable (`GEMINI_API_KEY`) via Spring's property placeholder `${GEMINI_API_KEY}`. It's never hardcoded or committed to Git. In production, you'd use a secrets manager like AWS Secrets Manager or Kubernetes Secrets.

### Q: What is @ConfigurationProperties?
**A:** It's a type-safe alternative to `@Value`. It maps a group of YAML properties (e.g., `gemini.api.*`) to a Java class with nested objects. Benefits: compile-time safety, IDE autocomplete, validation support, and grouping related config. The Spring team recommends it over scattered `@Value` annotations.

### Q: How do you parse the AI response?
**A:** I split the response by newlines and only keep lines that match the pattern `^\d+[.)-]\s*` (lines starting with a number prefix). This filters out any extra text the AI might add. I then strip the number prefix and validate the remaining text is at least 10 characters. If the AI returns more questions than expected, I truncate; if fewer, I log a warning.

---

*Previous: [Phase 2 — Session Management](PHASE_2_SESSION_MANAGEMENT.md) | Next: [Phase 4 — Real-Time Chat Interface](PHASE_4_CHAT_INTERFACE.md)*
