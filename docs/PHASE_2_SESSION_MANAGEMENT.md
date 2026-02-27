# Phase 2: Interview Session Management

> **Status:** ✅ Complete  
> **Goal:** Users can create interview sessions by choosing role, topic, and difficulty.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Entity Design & JPA](#entity-design--jpa)
3. [Repository Layer](#repository-layer)
4. [Service Layer Pattern](#service-layer-pattern)
5. [Controller & Form Handling](#controller--form-handling)
6. [DTO Pattern](#dto-pattern)
7. [Testing Strategy](#testing-strategy)
8. [Interview Q&A](#interview-qa)

---

## What Was Built

- **4 Enums** — InterviewRole, InterviewTopic, Difficulty, SessionStatus (with display names)
- **2 JPA Entities** — InterviewSession (OneToMany) ↔ Question (ManyToOne)
- **2 Repositories** — JpaRepository with custom query methods
- **1 DTO** — CreateSessionRequest with Bean Validation
- **1 Service** — InterviewService with full session lifecycle
- **1 Controller** — InterviewController with form handling + PRG pattern
- **3 Pages** — Create session form, session list, session detail
- **10 Unit Tests** — Using Mockito for isolated service testing

---

## Entity Design & JPA

### InterviewSession Entity
```java
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)  // Store as "SDE", not 0
    private InterviewRole role;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions;

    @PrePersist  // Auto-set timestamp before INSERT
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }
}
```

### Key JPA Concepts

| Annotation | Purpose |
|---|---|
| `@Entity` | Maps class to database table |
| `@Id` + `@GeneratedValue` | Auto-generated primary key |
| `@Enumerated(EnumType.STRING)` | Store enum as text, not ordinal number |
| `@OneToMany(mappedBy)` | Bidirectional relationship — "the other side owns the FK" |
| `CascadeType.ALL` | Save/delete session → auto save/delete its questions |
| `orphanRemoval = true` | Remove question from list → delete from DB |
| `@ManyToOne(fetch = LAZY)` | Don't load parent until explicitly accessed |
| `@PrePersist` / `@PreUpdate` | JPA lifecycle callbacks — auto-set timestamps |
| `@JoinColumn` | Names the foreign key column |

### Why @Enumerated(STRING) not ORDINAL?
- **ORDINAL** stores the position (0, 1, 2). If you reorder enum values, ALL existing data breaks.
- **STRING** stores the name ("EASY", "MEDIUM"). Safe to reorder, add, or remove values.

### Bidirectional Relationship
```
InterviewSession (1) ←→ (Many) Question
       ↑ mappedBy                  ↑ owns FK (session_id)
```
- **Session** has `@OneToMany(mappedBy = "session")` — it doesn't own the FK
- **Question** has `@ManyToOne` + `@JoinColumn(name = "session_id")` — it owns the FK
- Always use `addQuestion()` helper to maintain both sides of the relationship

---

## Repository Layer

```java
@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByStatus(SessionStatus status);
    List<InterviewSession> findAllByOrderByCreatedAtDesc();
}
```

### How Spring Data JPA Query Methods Work
Spring parses the method name and auto-generates SQL:

| Method Name | Generated SQL |
|---|---|
| `findByStatus(status)` | `SELECT * FROM interview_sessions WHERE status = ?` |
| `findAllByOrderByCreatedAtDesc()` | `SELECT * FROM interview_sessions ORDER BY created_at DESC` |
| `findBySessionIdAndAnsweredFalse()` | `WHERE session_id = ? AND answered = false` |
| `countBySessionIdAndAnsweredTrue()` | `SELECT COUNT(*) ... WHERE session_id = ? AND answered = true` |

No SQL, no boilerplate — just follow the naming convention.

---

## Service Layer Pattern

```java
@Service
@Transactional
public class InterviewService {
    private final InterviewSessionRepository sessionRepository;

    // Constructor injection (not @Autowired)
    public InterviewService(InterviewSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
}
```

### Why Constructor Injection > @Autowired?
| Constructor Injection | @Autowired (Field) |
|---|---|
| Dependencies are explicit | Hidden dependencies |
| Fields can be `final` (immutable) | Fields must be mutable |
| Works without Spring (easy testing) | Requires Spring or reflection |
| Fail-fast if dependency missing | May fail later at runtime |
| **Recommended by Spring team** | Legacy approach |

### @Transactional Explained
- Methods in this class run inside a database transaction
- If any exception occurs, ALL changes in that method are rolled back
- `@Transactional(readOnly = true)` on read methods — hints DB for optimization

### Session State Machine
```
NOT_STARTED → startSession() → IN_PROGRESS → completeSession() → COMPLETED
```
- Invalid transitions throw `IllegalStateException` (e.g., can't complete a NOT_STARTED session)

---

## Controller & Form Handling

### POST-Redirect-GET Pattern (PRG)
```java
@PostMapping("/new")
public String createSession(@Valid @ModelAttribute request, BindingResult result, ...) {
    if (result.hasErrors()) return "create-session";  // Show form again
    session = service.createSession(request);
    return "redirect:/interviews/" + session.getId();  // PRG!
}
```

**Why PRG?** If user submits a form and refreshes the browser:
- WITHOUT PRG: Form is resubmitted → duplicate data
- WITH PRG: Browser refreshes the GET redirect → safe

### @Valid + BindingResult
1. `@Valid` triggers validation annotations on the DTO (`@NotNull`, etc.)
2. `BindingResult` captures errors — MUST be right after `@Valid` parameter
3. If errors exist, re-render the form with error messages
4. If no errors, proceed with business logic

---

## DTO Pattern

```java
public class CreateSessionRequest {
    @NotNull(message = "Please select a role")
    private InterviewRole role;
    // ...
}
```

### Why DTO instead of Entity?
- **Security**: Prevents injecting unexpected fields (id, status, createdAt)
- **Validation**: Validate at the boundary (web layer), not deep inside
- **Decoupling**: Form shape ≠ Database shape (entity may have 15 fields, form has 3)
- **Clean API**: Controller receives exactly what it needs

---

## Testing Strategy

```java
@ExtendWith(MockitoExtension.class)  // No Spring context needed
class InterviewServiceTest {
    @Mock private InterviewSessionRepository sessionRepository;
    @InjectMocks private InterviewService interviewService;
}
```

### Unit Test vs Integration Test
| Unit Test | Integration Test |
|---|---|
| Tests ONE class in isolation | Tests multiple classes together |
| Mocks dependencies | Uses real dependencies |
| Fast (milliseconds) | Slower (needs Spring context, DB) |
| We use this for service layer | We'll use for controller layer |

### Mockito Key Concepts
| Method | Purpose |
|---|---|
| `@Mock` | Create a fake implementation of the interface |
| `@InjectMocks` | Create the real class, inject the mocks |
| `when(...).thenReturn(...)` | "When this method is called, return this" |
| `verify(mock, times(1))` | Assert the mock was called exactly once |
| `assertThrows(...)` | Assert an exception is thrown |

---

## Interview Q&A

### Q: Explain your entity relationship design.
**A:** InterviewSession and Question have a bidirectional OneToMany relationship. Session is the parent with `@OneToMany(mappedBy="session")`, and Question owns the FK with `@ManyToOne`. I use `CascadeType.ALL` so saving a session auto-saves questions, and `orphanRemoval=true` so removing questions from the list deletes them from the DB.

### Q: Why EnumType.STRING over ORDINAL?
**A:** ORDINAL stores position (0, 1, 2), so reordering or inserting enum values corrupts all existing data. STRING stores names ("EASY", "MEDIUM"), which are safe to reorder. The slight DB storage cost is worth the safety.

### Q: How does Spring Data JPA generate queries from method names?
**A:** Spring parses the method name at startup. `findByStatusOrderByCreatedAtDesc` → `WHERE status = ? ORDER BY created_at DESC`. It maps camelCase to column names, supports And, Or, Between, Like, etc. If the naming convention isn't sufficient, you can use `@Query` with JPQL.

### Q: Explain the Service Layer pattern.
**A:** The service layer encapsulates business logic between the controller and repository. Controllers handle HTTP concerns (request/response). Services handle business rules (validation, state transitions, computation). Repositories handle data access. This separation gives testability, reusability, and clean architecture.

### Q: What is constructor injection and why use it?
**A:** Dependencies are passed through the constructor instead of field injection with @Autowired. Benefits: dependencies are explicit, fields can be final (immutable), works without Spring container (easy unit testing), and fails fast if a dependency is missing. Spring team officially recommends this approach.

### Q: What is @Transactional?
**A:** It wraps the method in a database transaction. If the method completes normally, changes are committed. If an exception occurs, everything is rolled back. `readOnly=true` on read methods hints the database to optimize (skip dirty checking in Hibernate).

### Q: Explain your testing approach.
**A:** I use Mockito for unit testing the service layer. I mock the repository with `@Mock` and inject it into the service with `@InjectMocks`. This isolates the service logic without needing a database or Spring context. Tests verify: normal flow (create, get, start, complete), error cases (not found, invalid state), and edge cases (empty lists, score calculation).

### Q: What is the PRG pattern?
**A:** Post-Redirect-Get prevents duplicate form submissions. After a POST, instead of returning a view directly, I redirect to a GET URL. If the user refreshes, they refresh the GET (safe) instead of resubmitting the POST (creates duplicates).

---

*Previous: [Phase 1 — Project Setup](PHASE_1_PROJECT_SETUP.md) | Next: [Phase 3 — AI Integration](PHASE_3_AI_INTEGRATION.md)*
