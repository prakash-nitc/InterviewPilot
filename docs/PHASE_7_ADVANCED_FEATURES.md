# Phase 7: Advanced Features

> **Status:** ✅ Complete
> **Goal:** Add power-user features: question timer, PDF export, topic analytics, and session filtering.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Question Timer](#question-timer)
3. [PDF Export](#pdf-export)
4. [Topic Performance Breakdown](#topic-performance-breakdown)
5. [Session Filters](#session-filters)
6. [Testing Strategy](#testing-strategy)
7. [Interview Q&A](#interview-qa)

---

## What Was Built

| Feature | Description | Files |
|---|---|---|
| **Question Timer** | Countdown timer per question (Easy=3m, Medium=2m, Hard=90s) with auto-submit | `question-card.html`, `interview.html`, `style.css` |
| **PDF Export** | Download session report as PDF with scores, feedback, model answers | `PdfExportService.java`, `InterviewController.java`, `session-detail.html` |
| **Topic Breakdown** | Per-topic performance cards with grade badges and score bars | `TopicStats.java`, `InterviewService.java`, `sessions.html`, `style.css` |
| **Session Filters** | Tab-based filtering: All / In Progress / Completed / Not Started | `InterviewController.java`, `sessions.html`, `style.css` |

---

## Question Timer

### How It Works
1. Server sets a `data-timer` attribute on the question card based on difficulty level
2. JavaScript reads the timer value and starts a countdown
3. Timer pulses yellow at 30s remaining, red at 10s
4. At 0s, the form auto-submits (inserting a fallback answer if textarea is empty)
5. Timer resets when HTMX swaps in the next question

### Timer Values
| Difficulty | Timer |
|---|---|
| Easy | 3:00 (180s) |
| Medium | 2:00 (120s) |
| Hard | 1:30 (90s) |

### WHY client-side timer (not server-side)?
- **Simpler**: No WebSocket or polling needed.
- **Responsive**: Instant UI updates every second.
- **Sufficient**: This is practice, not an exam — no need for tamper-proof timing.

---

## PDF Export

### Architecture
```
User clicks "Download PDF" → GET /interviews/{id}/export
    → InterviewController.exportPdf()
        → PdfExportService.generateReport(session)
            → PDFBox generates A4 PDF in-memory
            → Returns byte[] via ResponseEntity with Content-Disposition
```

### Features
- A4 formatted report with session metadata header
- Per-question breakdown: question text, user answer, score, feedback, model answer
- Word-wrapping for long text
- Multi-page support (auto page breaks)
- Character sanitization (curly quotes, em-dashes, bullets → ASCII equivalents)

### WHY PDFBox (not iText)?
- **Already in our dependencies** (for resume parsing)
- **Apache License** — fully free (iText is AGPL)
- **Sufficient for text reports** — we don't need iText's advanced layout engine

---

## Topic Performance Breakdown

### How It Works
1. `InterviewService.getTopicBreakdown()` queries all completed sessions
2. Groups by `InterviewTopic` using `Collectors.groupingBy()`
3. Aggregates `totalScore` and `maxScore` per topic
4. Computes percentage and letter grade
5. Sorts by score descending (best topic first)

### UI
Topic cards displayed in a responsive grid on the sessions page. Each card shows:
- Topic name
- Letter grade badge (color-coded: green/yellow/red)
- Score progress bar
- Percentage and session count

---

## Session Filters

### Implementation
- Filter tabs rendered with simple `<a>` tags + query parameters
- Controller reads `@RequestParam(required = false) String status`
- Attempts `SessionStatus.valueOf(status)` — falls back to all sessions on invalid input
- Active tab highlighted via `activeFilter` model attribute + `th:classappend`

### WHY query params (not HTMX)?
- **Shareable URLs**: `/interviews?status=COMPLETED` is bookmarkable
- **Back button works**: No SPA state management needed
- **Simple**: No extra JavaScript or HTMX configuration

---

## Testing Strategy

### New Tests Added (5)
| Test | What It Verifies |
|---|---|
| `PdfExportServiceTest.generateReport_shouldProduceValidPdf` | Valid PDF output with %PDF header for completed session |
| `PdfExportServiceTest.generateReport_shouldHandleEmptySession` | Handles NOT_STARTED session without crashing |
| `PdfExportServiceTest.generateReport_shouldHandleSpecialCharacters` | Unicode sanitization (curly quotes, em-dash, bullets) |
| `InterviewServiceTest.getTopicBreakdown_shouldGroupByTopic` | Groups 3 sessions into 2 topics, sorted by score |
| `InterviewServiceTest.getTopicBreakdown_shouldReturnEmptyForNoSessions` | Empty list for no completed sessions |

**Total: 33 tests passing** (all green).

---

## Interview Q&A

### Q: How does the timer work with HTMX?
**A:** The timer is pure client-side JS. When the user submits an answer, HTMX swaps the `#question-area` with the next question card. The `htmx:afterSwap` event triggers `startTimer()`, which reads the new card's `data-timer` attribute and resets the countdown. The timer interval is cleared before each restart to prevent duplicates.

### Q: How do you generate PDFs in Java?
**A:** We use Apache PDFBox — an open-source library for creating and manipulating PDF files. Our `PdfExportService` creates a `PDDocument`, adds pages with content streams, and uses `PDType1Font` (built-in Helvetica) for text rendering. The key challenges are word-wrapping (manual calculation of text width) and character encoding (sanitizing Unicode to WinAnsi).

### Q: Why compute topic stats in Java instead of SQL?
**A:** For our scale, the number of completed sessions is small (tens to hundreds). Using `Collectors.groupingBy()` in Java is simpler to write, test, and maintain than a complex GROUP BY query with JOINs. If we had thousands of users, we'd add a native SQL query with `GROUP BY topic`.

### Q: How do filter tabs work without JavaScript?
**A:** They're plain `<a>` links with query parameters like `?status=COMPLETED`. The server reads the parameter, queries the matching sessions, and passes the active filter to Thymeleaf. Thymeleaf adds an `active` CSS class to highlight the current tab. No JS needed — it's just server-side rendering with URL-based state.

---

*Previous: [Phase 6 — Analytics](PHASE_6_ANALYTICS.md) | Next: [Phase 8 — Deployment & Polish](PHASE_8_DEPLOYMENT.md)*
