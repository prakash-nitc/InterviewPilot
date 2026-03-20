# Phase 4: Real-Time Chat Interface

> **Status:** ✅ Complete
> **Goal:** Build a chat-style interview page where users answer questions one at a time, powered by HTMX.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [HTMX — What & Why](#htmx--what--why)
3. [Architecture: Full Page vs Fragment](#architecture-full-page-vs-fragment)
4. [Interview Flow](#interview-flow)
5. [Thymeleaf Fragments](#thymeleaf-fragments)
6. [Service Layer Additions](#service-layer-additions)
7. [Testing Strategy](#testing-strategy)
8. [Interview Q&A](#interview-qa)

---

## What Was Built

- **interview.html** — Chat-style page with progress bar, answered question history, and current question
- **fragments/question-card.html** — Thymeleaf fragment returned by HTMX for partial page updates
- **3 new controller endpoints** — `/play`, `/answer`, `/complete`
- **3 new service methods** — `getCurrentQuestion`, `getAnsweredQuestions`, `submitAnswer`
- **Chat CSS** — Bubbles, animations, progress bar, scrollable history
- **4 new unit tests** — testing the new service methods

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
return "fragments/question-card :: questionCard(session=${session}, question=${question})";
```

### Fragment Syntax
```html
<!-- Define fragment -->
<div th:fragment="questionCard(session, question)" class="question-active-card">
    ...
</div>

<!-- Use fragment in another template -->
<div th:replace="~{fragments/question-card :: questionCard(session=${session}, question=${currentQuestion})}"></div>
```

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

## Testing Strategy

4 new tests added for Phase 4 methods:
- `getCurrentQuestion` → returns first unanswered question
- `getCurrentQuestion` → returns null when all answered
- `submitAnswer` → marks question answered and stores text
- `submitAnswer` → throws if already answered

Total: **19 tests passing** (14 service + 4 question gen + 1 context).

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

---

*Previous: [Phase 3 — AI Integration](PHASE_3_AI_INTEGRATION.md) | Next: [Phase 5 — Answer Evaluation & Scoring](PHASE_5_ANSWER_EVALUATION.md)*
