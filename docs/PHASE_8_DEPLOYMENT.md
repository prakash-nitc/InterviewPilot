# Phase 8: Deployment & Polish

> **Status:** ✅ Complete
> **Goal:** Production-ready deployment with Docker, PostgreSQL, custom error pages, and environment profiles.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Custom Error Pages](#custom-error-pages)
3. [Environment Profiles](#environment-profiles)
4. [Docker Deployment](#docker-deployment)
5. [UI Theme Overhaul](#ui-theme-overhaul)
6. [Bug Fixes](#bug-fixes)
7. [Interview Q&A](#interview-qa)

---

## What Was Built

| Feature | Description | Files |
|---|---|---|
| **Custom Error Pages** | Themed 404 and 500 error pages with animations | `error/404.html`, `error/500.html`, `style.css` |
| **Production Profile** | PostgreSQL config with env variables | `application-prod.yml`, `pom.xml` |
| **Docker Support** | Multi-stage Dockerfile + Compose with PostgreSQL | `Dockerfile`, `docker-compose.yml`, `.dockerignore` |
| **Light Theme** | Full UI redesign from dark to pastel gradient | `style.css` (all CSS variables + components) |
| **Progress Fix** | Dynamic progress bar tracking with follow-ups | `InterviewController.java`, `interview.html`, `question-card.html` |

---

## Custom Error Pages

### Implementation
Spring Boot automatically resolves error pages from `templates/error/{status}.html`:
- **404.html** — "Page Not Found" with search icon animation
- **500.html** — "Something Went Wrong" with warning icon

Both pages include the full navbar and use the app's design system. The error code is displayed with the gradient text effect.

### WHY custom error pages?
- **Professional UX**: Default Spring Boot whitelabel error pages look unprofessional
- **Navigation**: Users can easily return to the app from error pages
- **Branding**: Consistent look and feel across all states

---

## Environment Profiles

### Development (default)
```yaml
# application.yml
datasource: H2 in-memory
h2.console: enabled
thymeleaf.cache: false
stacktrace: always
```

### Production (`-Dspring.profiles.active=prod`)
```yaml
# application-prod.yml
datasource: PostgreSQL (env vars)
h2.console: disabled
thymeleaf.cache: true
stacktrace: never
```

### Environment Variables (Production)
| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `interviewpilot` | Database name |
| `DB_USERNAME` | `interviewpilot` | Database user |
| `DB_PASSWORD` | `interviewpilot` | Database password |
| `GROQ_API_KEY` | — | Groq API key (required) |
| `PORT` | `8080` | Server port |

### WHY profile-based config?
- **Separation**: Dev uses H2 (zero setup), prod uses PostgreSQL (persistent)
- **Security**: No stacktraces or debug consoles in production
- **12-Factor**: All secrets via environment variables, not hardcoded

---

## Docker Deployment

### Architecture
```
docker-compose.yml
├── app (InterviewPilot)
│   ├── Multi-stage build (JDK 17 → JRE 17 Alpine)
│   ├── Non-root user (appuser)
│   ├── Health check (wget → localhost:8080)
│   └── Profile: prod
└── db (PostgreSQL 16 Alpine)
    ├── Named volume (pgdata) for persistence
    └── Health check (pg_isready)
```

### Quick Start
```bash
# Set your API key
export GROQ_API_KEY=your-key-here

# Build and run
docker-compose up --build

# Access at http://localhost:8080
```

### WHY multi-stage build?
- **Stage 1 (builder)**: Uses full JDK to compile — `maven dependency:go-offline` caches deps in a separate layer
- **Stage 2 (runtime)**: Uses slim JRE-only image (~200MB vs ~400MB)
- **Result**: Faster builds (deps cached) and smaller production image

### WHY Alpine-based images?
- **Size**: Alpine images are ~5MB base vs ~80MB for Debian
- **Security**: Minimal attack surface (fewer packages installed)
- **Speed**: Faster pulls and deploys

---

## UI Theme Overhaul

### Changes
Transformed the entire UI from a dark navy theme to a light pastel gradient design:

| Element | Before | After |
|---|---|---|
| Background | `#0f172a` (navy) | Pastel gradient (blue→purple→pink) |
| Cards | Dark slate | Frosted white glass (backdrop-filter) |
| Text | Light gray | Dark slate |
| Navbar | Dark transparent | White glass with blur |
| Shadows | Heavy black | Soft indigo-tinted |
| Score colors | Neon (bright) | Deeper tones for readability |

### WHY light theme?
- **Accessibility**: Better readability with dark text on light background
- **Modern aesthetic**: Pastel gradients with glassmorphism are current design trends
- **Professional**: Cleaner, more polished appearance for portfolio showcase

---

## Bug Fixes

### Progress Bar Not Updating
**Problem**: The "Question X of Y" header showed static counts and didn't update when follow-up questions were dynamically added.

**Root Cause**: The progress text was rendered server-side on initial page load. HTMX only swapped the question card fragment, leaving the header unchanged.

**Fix**:
1. Added `data-current`, `data-total`, `data-progress` attributes to question card fragment
2. Controller now computes `totalQuestions` and `progressPercent` including follow-ups
3. JavaScript reads these data attributes on `htmx:afterSwap` and updates the header DOM

---

## Interview Q&A

### Q: How do custom error pages work in Spring Boot?
**A:** Spring Boot's `BasicErrorController` looks for templates at `templates/error/{statusCode}.html`. If a 404 occurs, it renders `error/404.html`. No explicit controller mapping needed — just place the files in the right directory.

### Q: Why use Docker Compose instead of just a Dockerfile?
**A:** The app depends on PostgreSQL. Compose orchestrates both services, handles startup ordering (via `depends_on` + health checks), and manages networking (the `app` service reaches `db` by hostname).

### Q: How does the multi-stage build help?
**A:** Stage 1 compiles the code with a full JDK (~400MB). Stage 2 copies only the JAR to a slim JRE image (~200MB). The final image doesn't contain Maven, source code, or build tools — making it smaller and more secure.

### Q: How does Spring handle profile-based configuration?
**A:** `application.yml` is always loaded. When `--spring.profiles.active=prod` is set, Spring also loads `application-prod.yml`, and any overlapping properties are overridden. This lets us keep dev defaults while customizing production settings.

---

*Previous: [Phase 7 — Advanced Features](PHASE_7_ADVANCED_FEATURES.md)*
