# Phase 6: Interview History & Analytics

> **Status:** ✅ Complete
> **Goal:** Add dashboard analytics, session-level results summary, and visual score tracking across the application.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Dashboard Stats Architecture](#dashboard-stats-architecture)
3. [Landing Page Stats](#landing-page-stats)
4. [Sessions Page Enhancements](#sessions-page-enhancements)
5. [Results Summary (Session Detail)](#results-summary-session-detail)
6. [Grading System](#grading-system)
7. [Testing Strategy](#testing-strategy)
8. [Interview Q&A](#interview-qa)

---

## What Was Built

- **DashboardStats DTO** — aggregated analytics: total sessions, completed, avg score %, grade, questions answered
- **Repository query** — `countByStatus()` for efficient status-based counting
- **Service method** — `getDashboardStats()` computes all analytics from completed sessions
- **Landing page** — hero stats now show live data (completed sessions, avg score, grade, questions)
- **Sessions page** — stats banner + color-coded score progress bars on each completed session card
- **Session detail** — results summary section with letter grade, total score, percentage, per-question breakdown bars
- **2 new tests** — dashboard stats computation and empty-state handling
- **Responsive CSS** — stats banner, score bars, results grid, per-question breakdown

---

## Dashboard Stats Architecture

```
HomeController / InterviewController
        │
        └── interviewService.getDashboardStats()
                │
                ├── sessionRepository.count()              → total sessions
                ├── sessionRepository.countByStatus(COMPLETED)   → completed count
                ├── sessionRepository.countByStatus(IN_PROGRESS) → in-progress count
                ├── sessionRepository.findByStatus(COMPLETED)    → all completed sessions
                │       │
                │       ├── Stream: sum totalScore, sum maxScore → avg %
                │       ├── computeGrade(avgPercent) → letter grade
                │       └── Stream: flatMap questions → count answered → total Q's
                │
                └── Returns DashboardStats DTO
```

### Why Compute in Service (Not Raw SQL)?
- **Business logic** (grade computation, percentage rounding) belongs in Java, not SQL.
- **Testable** with mock data — no database needed.
- **Simple** for small datasets — for 100s of sessions, in-memory aggregation is instant.
- For thousands of sessions, we'd use `@Query` with SQL aggregation. YAGNI for now.

---

## Landing Page Stats

The hero section now shows **live data** instead of static text:

| Stat | Source | Before | After |
|---|---|---|---|
| Sessions Completed | `stats.completedSessions` | "5+" (static) | Actual count |
| Avg Score | `stats.averageScorePercent` | "10+" (static) | e.g., "72.5%" |
| Overall Grade | `stats.bestGrade` | "AI" (static) | e.g., "B" |
| Questions Answered | `stats.totalQuestionsAnswered` | N/A | Actual count |

---

## Sessions Page Enhancements

### Stats Banner
A horizontal stats bar at the top showing Total, Completed, Avg Score, Grade. Only shown when there are sessions.

### Score Progress Bars
Each completed session card now shows a color-coded progress bar:
- **≥ 80%**: Green gradient
- **50-79%**: Yellow gradient
- **< 50%**: Red gradient

---

## Results Summary (Session Detail)

For completed sessions, a results card appears with:

### Results Grid (4 cards)
1. **Overall Grade** — Large letter (A+, A, B, C, D, F) with gradient background
2. **Total Score** — e.g., "35/50"
3. **Score Percentage** — e.g., "70%"
4. **Questions Count** — e.g., "5"

### Per-Question Breakdown
Horizontal bar chart showing each question's score with color-coded fills.

---

## Grading System

| Percentage | Grade |
|---|---|
| ≥ 90% | A+ |
| ≥ 80% | A |
| ≥ 70% | B |
| ≥ 60% | C |
| ≥ 50% | D |
| < 50% | F |

---

## Testing Strategy

2 new tests in `InterviewServiceTest`:
- `getDashboardStats` — verifies correct computation with a completed session (score 35/50 = 70% = B)
- `getDashboardStats` — handles empty state (no completed sessions)

Also fixed:
- `startSession` test updated for 4-param `generateQuestions` (resume support)
- `QuestionGenerationServiceTest` updated for 5-param `buildPrompt`
- Added `FollowUpService` mock to `InterviewServiceTest`

Total: **28 tests passing** (16 service + 7 evaluation + 4 question gen + 1 context).

---

## Interview Q&A

### Q: How do you compute analytics?
**A:** The `getDashboardStats()` method queries the repository for completed sessions, then uses Java Streams to aggregate scores: sum all `totalScore` values, sum all `maxScore` values, compute the percentage, and derive a letter grade. This is computed on-demand, not cached.

### Q: Why not use SQL aggregation (SUM, AVG)?
**A:** For our scale (tens to hundreds of sessions), in-memory computation with Streams is simpler to write, test, and maintain. If we scaled to thousands of users, we'd add a `@Query` with SQL `SUM()` and `AVG()`. This is the YAGNI principle — don't optimize prematurely.

### Q: How does the grading system work?
**A:** We compute `(totalScore / maxScore) * 100` to get a percentage, then map it to a letter grade using static thresholds (90+ = A+, 80+ = A, etc.). The grade is computed in `DashboardStats.computeGrade()` — a pure static method that's easy to test.

### Q: Why show stats on both the landing page and sessions page?
**A:** Different contexts, same data. The landing page shows stats to motivate users ("you've completed 5 sessions!"). The sessions page shows the same stats for quick reference alongside the full session list. Both call the same `getDashboardStats()` method.

---

*Previous: [Phase 5 — Answer Evaluation](PHASE_5_ANSWER_EVALUATION.md) | Next: [Phase 7 — Advanced Features](PHASE_7_ADVANCED_FEATURES.md)*
