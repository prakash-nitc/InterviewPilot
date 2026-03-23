# Phase 4: Real-Time Chat Interface

> **Status:** ✅ Complete (with Bug Fixes & Groq Migration)
> **Goal:** Build a chat-style interview page where users answer questions one at a time, powered by HTMX.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [HTMX — What & Why](#htmx--what--why)
3. [Architecture: Full Page vs Fragment](#architecture-full-page-vs-fragment)
4. [Interview Flow](#interview-flow)
5. [Thymeleaf Fragments](#thymeleaf-fragments)
6. [Service Layer Additions](#service-layer-additions)
7. [Challenges Faced & How We Fixed Them](#challenges-faced--how-we-fixed-them)
8. [Groq API Migration](#groq-api-migration)
9. [Testing Strategy](#testing-strategy)
10. [Interview Q&A](#interview-qa)

---

## What Was Built

- **interview.html** — Chat-style page with progress bar, answered question history, and current question
- **fragments/question-card.html** — Thymeleaf fragment returned by HTMX for partial page updates
- **3 new controller endpoints** — `/play`, `/answer`, `/complete`
- **3 new service methods** — `getCurrentQuestion`, `getAnsweredQuestions`, `submitAnswer`
- **Chat CSS** — Bubbles, animations, progress bar, scrollable history
- **4 new unit tests** — testing the new service methods
- **Groq API migration** — Switched from Google Gemini to Groq (OpenAI-compatible format)

---

## HTMX — What & Why

### What is HTMX?
HTMX lets you add dynamic behavior to HTML using **attributes** instead of writing JavaScript. It makes HTTP requests and swaps HTML fragments directly into the DOM.

```html
<!-- Normal form: full page reload -->
<form action="/submit" method="post">...</form>

<!-- HTMX form: partial update, no reload -->
<form hx-post="/submit" hx-target="#result" hx-swap="innerHTML">...</form>
```

### Why HTMX instead of React/Angular?
| HTMX | React/Angular/Vue |
|---|---|
| No build step, no npm | Requires npm, webpack, babel |
| Server returns HTML | Server returns JSON, client renders |
| Works with Thymeleaf | Needs separate frontend project |
| 14KB library (CDN) | 100KB+ framework |
| Perfect for MPA apps | Better for SPA apps |

### Key HTMX Attributes Used
| Attribute | Purpose | Example |
|---|---|---|
| `hx-post` | Make POST request on submit | `hx-post="/interviews/1/answer"` |
| `hx-target` | Which element to update | `hx-target="#question-area"` |
| `hx-swap` | How to update the target | `hx-swap="innerHTML"` |

---

## Architecture: Full Page vs Fragment

```
Traditional (full page reload):
  User clicks → Browser sends request → Server returns FULL HTML page → Browser replaces everything

HTMX (partial update):
  User clicks → HTMX sends AJAX request → Server returns HTML FRAGMENT → HTMX replaces ONLY the target div
```

### How Our Answer Flow Works
1. User types answer in textarea
2. Clicks "Submit Answer" → HTMX sends `POST /interviews/{id}/answer`
3. Controller saves answer, gets next question
4. Returns **only** the question card fragment (not the full page)
5. HTMX replaces `#question-area` with the new card
6. User sees the next question — **no page flicker, no reload**

---

## Interview Flow

```
Create Session → Begin Interview → [AI generates questions]
        ↓
   /play page renders
        ↓
   Show Question 1 → User answers → Submit via HTMX
        ↓
   Show Question 2 → User answers → Submit via HTMX
        ↓
   ... repeat for all questions ...
        ↓
   Show "All Done" card → Complete Interview → Session Detail page
```

---

## Thymeleaf Fragments

### What is a Fragment?
A reusable, partial piece of HTML. Instead of returning a full page, the controller can return **just** a fragment.

```java
// Full page render:
return "interview";

// Fragment render (for HTMX):
return "fragments/question-card :: questionCard";
```

### Fragment Syntax
```html
<!-- Define fragment (no parameters — uses model attributes directly) -->
<div th:fragment="questionCard" class="question-active-card">
    <p th:text="${question.questionText}">...</p>
    <form th:hx-post="@{/interviews/{id}/answer(id=${interviewSession.id})}">
        ...
    </form>
</div>

<!-- Use fragment in another template -->
<div th:replace="~{fragments/question-card :: questionCard}"></div>
```

> **Important:** Fragments returned from controllers access model attributes directly via `${}`.
> Do NOT use parameterized fragments like `th:fragment="name(param)"` when returning from controllers.

---

## Service Layer Additions

### getCurrentQuestion
```java
public Question getCurrentQuestion(Long sessionId) {
    return session.getQuestions().stream()
            .filter(q -> !q.isAnswered())
            .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
            .findFirst()
            .orElse(null);  // null = all done
}
```
**Stream API:** filter → sort → findFirst. Functional programming in Java.

### submitAnswer
```java
public Question submitAnswer(Long sessionId, Long questionId, String answer) {
    question.setUserAnswer(answer);
    question.setAnswered(true);
    sessionRepository.save(session);
    return question;
}
```
**Idempotency guard:** Throws `IllegalStateException` if already answered.

---

## Challenges Faced & How We Fixed Them

### Challenge 1: Thymeleaf Reserved Word `session` (500 Error on All Pages)

**Symptom:** Every page that displayed session data (session list, session detail, interview play) crashed with a 500 error.

**Root Cause:** In Thymeleaf, `session` is a **reserved word** that refers to the `HttpSession` object. Our controller was doing:
```java
// ❌ BROKEN — "session" is reserved in Thymeleaf
model.addAttribute("session", interviewSession);
```
And templates were iterating:
```html
<!-- ❌ BROKEN — "session" as loop variable conflicts with HttpSession -->
<a th:each="session : ${sessions}">...</a>
```

**The Fix:**
```java
// ✅ FIXED — use a non-reserved name
model.addAttribute("interviewSession", session);
```
```html
<!-- ✅ FIXED — renamed loop variable to "s" -->
<a th:each="s : ${sessions}">...</a>
```

**Files fixed:** `InterviewController.java`, `session-detail.html`, `sessions.html`, `interview.html`, `question-card.html`

**Lesson Learned:** Thymeleaf reserves several variable names: `session`, `param`, `application`, `request`. Never use them as model attribute names or loop variables. This is poorly documented and easy to miss.

---

### Challenge 2: Find-and-Replace Missed Negation Patterns

**Symptom:** After renaming `session` → `interviewSession`, some pages still crashed.

**Root Cause:** The find-and-replace for `${session.` did NOT catch `${!session.` (with the `!` negation operator):
```html
<!-- ❌ This was MISSED by find-and-replace -->
<div th:if="${!session.questions.isEmpty()}">
```

**The Fix:** A second, more thorough pass to catch `!session.` patterns:
```html
<!-- ✅ Fixed -->
<div th:if="${!interviewSession.questions.isEmpty()}">
```

**Lesson Learned:** When doing bulk find-and-replace in templates, always verify with a comprehensive search for ALL patterns of the old variable name, including negation (`!`), method chains, and embedded references. Use `grep` or `Select-String` to scan all templates after the rename.

---

### Challenge 3: Old Templates Cached in `target/classes`

**Symptom:** Template files were correct on disk, but the server still threw errors referencing old `session` variable names.

**Root Cause:** Spring Boot's `mvnw spring-boot:run` copies resources to `target/classes/`. When templates are edited directly (not through Maven), the `target/classes` directory still has the old versions.

**The Fix:** Use `mvnw clean spring-boot:run` instead of just `mvnw spring-boot:run`:
```powershell
# ❌ May serve stale templates
.\mvnw.cmd spring-boot:run

# ✅ Deletes target/ first, forces fresh copy
.\mvnw.cmd clean spring-boot:run
```

**Lesson Learned:** Always use `clean` when debugging template issues. The `target/classes` cache can mask your fixes and waste hours of debugging.

---

### Challenge 4: JPA Lazy Loading (`LazyInitializationException`)

**Symptom:** Accessing `session.questions` in Thymeleaf templates sometimes failed.

**Root Cause:** JPA's `@OneToMany` uses **LAZY** loading by default. The `questions` collection is not loaded from the database until accessed. If the Hibernate session is closed (after the `@Transactional` method returns), accessing `questions` in the template throws `LazyInitializationException`.

**The Fix:** Since an interview has only 5-10 questions (small collection), we switched to EAGER loading:
```java
// ❌ Default: LAZY — fails in templates
@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)

// ✅ Fixed: EAGER — always loads questions with the session
@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
```

**Lesson Learned:** For small collections that are always needed (e.g., questions in a session), EAGER fetch is simpler and avoids the Open Session In View anti-pattern. For large or optional collections, use `JOIN FETCH` in queries instead.

---

### Challenge 5: Google Gemini API Quota Exhausted (429 Error)

**Symptom:** Clicking "Begin Interview" showed: `429 Too Many Requests: "You exceeded your current quota"`

**Root Cause:** Google Gemini's free tier has strict rate limits (requests per minute/day). After several test runs, the quota was exhausted.

**The Fix:** Migrated from Gemini to **Groq** (see [Groq API Migration](#groq-api-migration) section below).

**Lesson Learned:** Always have a fallback AI provider. Groq's free tier is more generous and uses the industry-standard OpenAI format, making it easy to switch between providers.

---

## Groq API Migration

### What Changed

We switched from Google Gemini to **Groq** — a fast AI inference platform that uses the **OpenAI-compatible** chat completions API format.

### Why Groq?
| Feature | Gemini | Groq |
|---|---|---|
| API Format | Proprietary (contents/parts) | OpenAI-compatible (model/messages) |
| Auth | Query param (`?key=`) | Bearer token (industry standard) |
| Free Tier | Strict limits, exhausted quickly | More generous limits |
| Speed | ~2-3s response | ~0.5-1s response (very fast) |
| Model | `gemini-2.0-flash` | `llama-3.3-70b-versatile` |

### Files Modified

| File | What Changed |
|---|---|
| `application.yml` | URL → `api.groq.com`, env var → `GROQ_API_KEY`, added `model` |
| `GeminiProperties.java` | Added `model` field to `Api` inner class |
| `GeminiRequest.java` | Rewrote for OpenAI format: `{model, messages}` |
| `GeminiResponse.java` | Rewrote for OpenAI format: `{choices: [{message: {content}}]}` |
| `GeminiApiClient.java` | Bearer auth header instead of query param key |

### Old vs New Request Format
```json
// ❌ OLD (Gemini proprietary format)
{
  "contents": [
    { "parts": [{ "text": "your prompt" }] }
  ]
}

// ✅ NEW (OpenAI-compatible format — works with Groq, OpenAI, etc.)
{
  "model": "llama-3.3-70b-versatile",
  "messages": [
    { "role": "user", "content": "your prompt" }
  ]
}
```

### Old vs New Response Format
```json
// ❌ OLD (Gemini)
{
  "candidates": [{ "content": { "parts": [{ "text": "response" }] } }]
}

// ✅ NEW (OpenAI-compatible)
{
  "choices": [{ "message": { "role": "assistant", "content": "response" } }]
}
```

### Old vs New Auth
```java
// ❌ OLD (Gemini — API key as query param)
String url = properties.getUrl() + "?key=" + properties.getKey();
restClient.post().uri(url).body(request).retrieve();

// ✅ NEW (Groq — Bearer token in header)
restClient.post()
    .uri(properties.getUrl())
    .header("Authorization", "Bearer " + properties.getKey())
    .body(request)
    .retrieve();
```

### How to Run with Groq
```powershell
$env:GROQ_API_KEY="your-groq-api-key-here"
.\mvnw.cmd clean spring-boot:run
```

---

## Testing Strategy

4 new tests added for Phase 4 methods:
- `getCurrentQuestion` → returns first unanswered question
- `getCurrentQuestion` → returns null when all answered
- `submitAnswer` → marks question answered and stores text
- `submitAnswer` → throws if already answered

Total: **19 tests passing** (14 service + 4 question gen + 1 context).

> **Note:** The Groq API migration did NOT break any tests because tests mock `GeminiApiClient.generateContent()` — they never hit the actual API. This validates our architecture decision to separate the HTTP client from the business logic.

---

## Interview Q&A

### Q: What is HTMX and why did you use it?
**A:** HTMX is a 14KB JavaScript library that adds AJAX behavior through HTML attributes like `hx-post` and `hx-target`. I chose it because it works seamlessly with Spring MVC + Thymeleaf — the server returns HTML fragments instead of JSON, so there's no need for a separate frontend framework. It gave us a smooth, no-reload interview experience without the complexity of React.

### Q: How does the partial page update work?
**A:** When the user submits an answer, HTMX sends a POST request to `/interviews/{id}/answer`. The controller processes the answer, gets the next question, and returns a Thymeleaf fragment (just the question card HTML, not a full page). HTMX takes this fragment and swaps it into the `#question-area` div, so only that section of the page updates.

### Q: What is a Thymeleaf fragment?
**A:** A fragment is a named, reusable piece of HTML defined with `th:fragment="name"`. Instead of returning an entire page, a controller can return a specific fragment using the syntax `"templateName :: fragmentName"`. This is essential for HTMX because it expects HTML snippets, not full pages.

### Q: How do you track the current question?
**A:** I don't store a "current question index" in the database. Instead, `getCurrentQuestion()` uses Java Streams to filter the session's questions for the first one where `answered == false`, sorted by `orderIndex`. This is derived state — always consistent, no sync issues.

### Q: Why not use WebSocket instead of HTMX?
**A:** WebSockets are bidirectional and great for real-time push notifications (chat apps, stock tickers). For our use case — user submits → server responds — the standard HTTP request-response model is simpler. HTMX gives us the smooth UX of WebSockets with the simplicity of regular HTTP.

### Q: What was the biggest bug you faced in this phase, and how did you debug it?
**A:** The biggest challenge was the Thymeleaf reserved word `session` causing 500 errors across all pages. The error message was `TemplateInputException` which was misleading — it looked like a template parsing error, not a variable naming conflict. I debugged it by:
1. Enabling `server.error.include-stacktrace: always` and `include-message: always` in `application.yml`
2. Reading the full stack trace in the browser, which showed the exact SpEL expression and line number
3. Discovering that `session` is reserved in Thymeleaf (it refers to `HttpSession`)
4. Renaming all occurrences to `interviewSession` across the controller and all templates

### Q: Why did you switch from Gemini to Groq?
**A:** Google Gemini's free tier quota got exhausted during development. Groq was chosen because: (1) it has a generous free tier, (2) it uses the OpenAI-compatible API format (industry standard), making it easy to switch between providers, and (3) it's extremely fast. The migration only required changing 5 files — the DTOs, the API client, and the config.

### Q: What is EAGER vs LAZY loading in JPA?
**A:** LAZY loading (default for `@OneToMany`) only loads related entities when they're explicitly accessed. This saves memory but can cause `LazyInitializationException` if the Hibernate session is closed. EAGER loading fetches related entities immediately with the parent. I used EAGER for `questions` because: (a) there are only 5-10 questions per session, and (b) every page that shows a session also shows its questions.

### Q: Why didn't your tests break when you switched from Gemini to Groq?
**A:** Because of **separation of concerns**. The tests mock `GeminiApiClient.generateContent()` which returns a plain `String`. The tests never construct `GeminiRequest` or parse `GeminiResponse` — that's inside the client. So when I changed the request/response format for Groq, the test contract (String → String) stayed the same. This proves why separating HTTP concerns from business logic matters.

---

*Previous: [Phase 3 — AI Integration](PHASE_3_AI_INTEGRATION.md) | Next: [Phase 5 — Answer Evaluation & Scoring](PHASE_5_ANSWER_EVALUATION.md)*
