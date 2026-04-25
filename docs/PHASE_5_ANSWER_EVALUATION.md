# Phase 5: AI-Powered Answer Evaluation & Scoring

> **Status:** ✅ Complete
> **Goal:** After a user submits an answer, the AI evaluates it — providing a score (0-10), detailed feedback, and a model answer.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [How Answer Evaluation Works](#how-answer-evaluation-works)
3. [Prompt Engineering for Evaluation](#prompt-engineering-for-evaluation)
4. [JSON Response Parsing](#json-response-parsing)
5. [Scoring Rubric](#scoring-rubric)
6. [Architecture](#architecture)
7. [Template Changes](#template-changes)
8. [Testing Strategy](#testing-strategy)
9. [Interview Q&A](#interview-qa)

---

## What Was Built

- **AnswerEvaluationService.java** — Prompt engineering + AI call + JSON parsing for scoring
- **EvaluationResult.java** — DTO for structured evaluation data (score, feedback, modelAnswer)
- **InterviewService.submitAnswer()** — Updated to trigger evaluation after saving answer
- **interview.html** — Chat history now shows score badges, feedback, and model answers
- **session-detail.html** — Detail page shows model answers alongside scores/feedback
- **style.css** — Color-coded score badges (green/yellow/red), evaluation card styling
- **7 new unit tests** — JSON parsing, markdown fences, malformed responses, fallbacks

---

## How Answer Evaluation Works

```
User submits answer → InterviewService.submitAnswer()
       ↓
1. Save user answer to Question entity
2. Call AnswerEvaluationService.evaluateAnswer()
       ↓
3. Build evaluation prompt (question + answer + context)
4. Call Groq API via GeminiApiClient
5. Parse JSON response → EvaluationResult
       ↓
6. Set score, feedback, modelAnswer on Question entity
7. Save to database
8. Return next question card via HTMX
```

### Why Evaluate Inline (Not Async)?
- The user **waits for the next question anyway** (HTMX swap).
- Groq API is very fast (~0.5-1s response time).
- Keeps the flow simple — no background jobs, no polling, no WebSockets.
- If evaluation fails, the answer is still saved — graceful degradation.

---

## Prompt Engineering for Evaluation

### The Evaluation Prompt
```
You are an experienced technical interviewer evaluating a candidate's answer.

Interview Context:
- Role: Software Development Engineer
- Topic: Java Programming
- Difficulty: Medium

Question: What is polymorphism?

Candidate's Answer: Polymorphism is when objects can take different forms...

Evaluate the answer using this rubric:
- 0-3: Poor — Incorrect, irrelevant, or shows no understanding
- 4-5: Below Average — Partially correct but missing key concepts
- 6-7: Average — Correct basics but lacks depth or has minor errors
- 8: Good — Mostly correct with good understanding, minor gaps
- 9-10: Excellent — Comprehensive, accurate, well-structured answer

Respond with ONLY valid JSON in this exact format:
{"score": <0-10>, "maxScore": 10, "feedback": "...", "modelAnswer": "..."}
```

### Key Prompt Design Decisions

| Decision | Why |
|---|---|
| Provide interview context (role, topic, difficulty) | AI calibrates expectations — Easy questions need simpler answers |
| Explicit scoring rubric | Without it, AI scores inconsistently (always 7-8 or always 3-4) |
| Demand JSON output | Machine-parseable — no regex gymnastics needed |
| "ONLY valid JSON, no markdown" | Reduces (but doesn't eliminate) code fence wrapping |
| Include `modelAnswer` field | Provides learning value — user sees the ideal answer |

---

## JSON Response Parsing

### The Challenge
AI models don't always return clean JSON. Common issues:

```
// ✅ Clean JSON (ideal)
{"score": 7, "maxScore": 10, "feedback": "...", "modelAnswer": "..."}

// ⚠️ Wrapped in markdown fences
```json
{"score": 7, ...}
```

// ⚠️ Has preamble text
Here's my evaluation:
{"score": 7, ...}

// ❌ Not JSON at all
The answer is good. I'd give it a 7 out of 10.
```

### Parsing Strategy
```java
// 1. Strip markdown code fences
text = text.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");

// 2. Find the JSON object (first { to last })
int start = text.indexOf('{');
int end = text.lastIndexOf('}');
String json = text.substring(start, end + 1);

// 3. Parse with Jackson
JsonNode root = objectMapper.readTree(json);

// 4. Extract fields with defaults
int score = root.has("score") ? root.get("score").asInt() : 5;

// 5. Clamp score to valid range
score = Math.max(0, Math.min(score, maxScore));
```

### Fallback Handling
If parsing fails completely, we return a **neutral fallback** (score: 5/10) so the interview can continue without crashing.

---

## Scoring Rubric

| Score | Level | Description |
|---|---|---|
| 0-3 | 🔴 Poor | Incorrect, irrelevant, or no understanding |
| 4-5 | 🟠 Below Average | Partially correct, missing key concepts |
| 6-7 | 🟡 Average | Correct basics, lacks depth or minor errors |
| 8 | 🟢 Good | Mostly correct, good understanding, minor gaps |
| 9-10 | 🟢 Excellent | Comprehensive, accurate, well-structured |

### Visual Indicators (CSS)
- **Score ≥ 8**: Green badge (`#4ade80`)
- **Score 5-7**: Yellow badge (`#facc15`)
- **Score < 5**: Red badge (`#f87171`)

---

## Architecture

```
InterviewService.submitAnswer()
        │
        ├── 1. Save answer to Question entity
        │
        ├── 2. AnswerEvaluationService.evaluateAnswer()
        │           │
        │           ├── buildEvaluationPrompt() → prompt string
        │           ├── GeminiApiClient.generateContent() → AI response
        │           └── parseEvaluation() → EvaluationResult DTO
        │
        ├── 3. Set score/feedback/modelAnswer on Question
        │
        └── 4. Save to database
```

### Why a Separate DTO (EvaluationResult)?
- **Clean separation**: Parsing logic returns structured data, doesn't touch JPA entities.
- **Testable**: We can verify parsing output independently.
- **Reusable**: If we ever evaluate answers outside a session context.

### Why Separate from QuestionGenerationService?
- **Single Responsibility**: Question generation and answer evaluation are different domain operations.
- **Different prompts**: Each has its own prompt engineering concerns.
- **Same HTTP client**: Both reuse `GeminiApiClient` — that's the shared infrastructure.

---

## Template Changes

### Chat View (interview.html)
After each answer bubble, an evaluation card appears with:
- Color-coded score badge (e.g., `8/10` in green)
- "AI Evaluation" label
- Feedback text (2-3 sentences)
- Model answer (italic, below a divider)

### Session Detail View (session-detail.html)
Each question card now shows:
- User's answer
- Score (e.g., `7/10`)
- AI feedback
- Model answer

---

## Testing Strategy

**7 new tests** in `AnswerEvaluationServiceTest`:
- Parse valid JSON response
- Handle markdown code fences (`\`\`\`json ... \`\`\``)
- Handle preamble text before JSON
- Return fallback on malformed response
- Return fallback on API failure
- Clamp out-of-range scores
- Build prompt includes all context

**1 updated test** in `InterviewServiceTest`:
- `submitAnswer` now verifies evaluation is triggered and score/feedback are set

Total: **26 tests passing** (14 service + 7 evaluation + 4 question gen + 1 context).

---

## Interview Q&A

### Q: How does the AI evaluate user answers?
**A:** When a user submits an answer, the `AnswerEvaluationService` builds a prompt that includes the question, the user's answer, and the interview context (role, topic, difficulty). It sends this to the Groq API and receives a JSON response containing a score (0-10), feedback, and a model answer. The score and feedback are saved to the `Question` entity.

### Q: Why did you ask the AI to return JSON instead of natural language?
**A:** JSON is machine-parseable — I can extract the score as an integer, the feedback as a string, etc. Natural language would require regex or NLP to extract the score ("I'd give it a 7 out of 10"), which is fragile. JSON gives us structured, reliable output.

### Q: What happens if the AI returns invalid JSON?
**A:** The `parseEvaluation()` method has multiple layers of defense: (1) it strips markdown code fences, (2) it extracts just the JSON portion from any surrounding text, (3) it uses Jackson for robust parsing. If all parsing fails, it returns a fallback result (score: 5/10) so the interview continues without crashing.

### Q: Why evaluate answers inline instead of asynchronously?
**A:** Three reasons: (1) the user is already waiting for the next question to appear via HTMX, so the delay is natural, (2) Groq API responds in ~0.5-1 second, which is fast enough, and (3) synchronous evaluation keeps the code simple — no background jobs, no polling, no message queues.

### Q: How do you ensure consistent scoring?
**A:** I provide a **scoring rubric** in the prompt (0-3: Poor, 4-5: Below Average, 6-7: Average, 8: Good, 9-10: Excellent). Without this, the AI tends to score inconsistently. I also clamp scores to the valid range (0 to maxScore) in code to prevent the AI from returning values like 15/10.

### Q: Why is AnswerEvaluationService separate from QuestionGenerationService?
**A:** Single Responsibility Principle. Question generation and answer evaluation are different domain operations with different prompts, different output formats, and different parsing logic. They share the same HTTP client (`GeminiApiClient`), which is the infrastructure layer. This also makes each service independently testable.

### Q: What is the EvaluationResult DTO and why not set fields directly on the Question entity?
**A:** `EvaluationResult` is a plain data object that holds the parsed evaluation (score, feedback, modelAnswer). It separates the parsing concern from the entity mutation. The parsing logic creates an `EvaluationResult`, and the calling code (`InterviewService`) decides whether/how to apply it to the entity. This makes the parsing independently testable and reusable.

---

*Previous: [Phase 4 — Chat Interface](PHASE_4_CHAT_INTERFACE.md) | Next: [Phase 6 — Interview History & Analytics](PHASE_6_ANALYTICS.md)*
