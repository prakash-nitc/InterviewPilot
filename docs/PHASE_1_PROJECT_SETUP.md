# Phase 1: Project Setup & Foundation

> **Status:** ✅ Complete  
> **Goal:** Initialize the InterviewPilot Spring Boot project with clean structure, landing page, and GitHub repo.

---

## Table of Contents
1. [What Was Built](#what-was-built)
2. [Tech Stack — Why Each Choice](#tech-stack--why-each-choice)
3. [Project Structure Explained](#project-structure-explained)
4. [Key Files Walkthrough](#key-files-walkthrough)
5. [Spring Boot Fundamentals](#spring-boot-fundamentals)
6. [Thymeleaf Basics](#thymeleaf-basics)
7. [Configuration Deep Dive](#configuration-deep-dive)
8. [Interview Q&A](#interview-qa)

---

## What Was Built

In Phase 1, we created the foundation:
- A **Spring Boot 3.5** project using **Spring Initializr**
- A clean **layered package structure** (controller, service, model, repository, dto, config)
- A polished **landing page** using Thymeleaf + CSS
- **Application configuration** in YAML format with H2 database
- A **context load test** verifying the app boots correctly
- **README.md** with project overview and architecture

---

## Tech Stack — Why Each Choice

### Java 17 + Spring Boot 3.5
- **Why Java?** Most widely used enterprise language. NIT campus placements heavily feature Java roles.
- **Why Spring Boot?** It's the de-facto standard for Java web apps. Auto-configuration eliminates boilerplate. Embedded Tomcat means no separate server setup.
- **Why 3.5?** Latest stable version supporting Java 17. Spring Boot 4.x requires Java 21+.

### Maven (via Maven Wrapper)
- **What is Maven?** A build tool that manages dependencies, compiles code, runs tests, and packages the app.
- **What is Maven Wrapper (`mvnw`)?** A script bundled with the project so anyone can build it WITHOUT installing Maven globally. They just run `./mvnw` instead of `mvn`.
- **Why Maven over Gradle?** Maven is more common in enterprise Java. XML-based `pom.xml` is explicit and easy to read.

### Thymeleaf
- **What is it?** A server-side Java template engine. You write HTML with special `th:` attributes, and Spring fills in the data before sending to the browser.
- **Why Thymeleaf?** Keeps everything in one Spring Boot app (no separate frontend build). Templates are natural HTML — you can open them in a browser even without the server.
- **Key difference from JSP:** Thymeleaf templates are valid HTML. JSP uses embedded Java code which is harder to maintain.

### H2 Database
- **What is it?** An in-memory Java SQL database. Data is stored in RAM and disappears when the app stops.
- **Why H2 for dev?** Zero setup — no installation, no configuration, no external database server needed. Perfect for development and testing.
- **Production plan:** Switch to PostgreSQL with just a config change (no code changes needed thanks to JPA abstraction).

### HTMX (will be used in Phase 4)
- **What is it?** A JavaScript library that lets you make AJAX requests using HTML attributes instead of writing JavaScript.
- **Why?** Gives SPA-like experience (partial page updates, no full reloads) without any JavaScript framework.

---

## Project Structure Explained

```
InterviewPilot/
├── pom.xml                        # Maven config — dependencies, build settings
├── mvnw / mvnw.cmd                # Maven Wrapper scripts (Linux/Windows)
├── .mvn/                          # Maven Wrapper config files
├── src/
│   ├── main/
│   │   ├── java/com/prakash/interviewpilot/
│   │   │   ├── InterviewpilotApplication.java   # Entry point — starts Spring Boot
│   │   │   ├── config/            # Configuration classes (security, WebSocket, etc.)
│   │   │   ├── controller/        # HTTP controllers — handle web requests
│   │   │   │   └── HomeController.java
│   │   │   ├── dto/               # Data Transfer Objects — shapes for API data
│   │   │   ├── model/             # JPA Entities — database table mappings
│   │   │   ├── repository/        # Data access layer — database queries
│   │   │   └── service/           # Business logic layer
│   │   └── resources/
│   │       ├── application.yml    # App configuration
│   │       ├── templates/         # Thymeleaf HTML templates
│   │       │   └── index.html
│   │       └── static/            # Static assets (CSS, JS, images)
│   │           └── css/style.css
│   └── test/                      # Test classes
│       └── java/com/prakash/interviewpilot/
│           └── InterviewpilotApplicationTests.java
├── docs/                          # Phase documentation
│   └── PHASE_1_PROJECT_SETUP.md   # This file
└── README.md
```

### Why This Package Structure? (Layered Architecture)

This follows the **Layered Architecture Pattern** — each layer has a specific responsibility:

| Package | Responsibility | Depends On |
|---|---|---|
| `controller/` | Handle HTTP requests, return views | `service/` |
| `service/` | Business logic, orchestration | `repository/`, external APIs |
| `repository/` | Database access (CRUD operations) | `model/` |
| `model/` | JPA entities (database tables) | Nothing |
| `dto/` | Data transfer shapes (API request/response) | Nothing |
| `config/` | App configuration (security, WebSocket, etc.) | Nothing |

**Why not put everything in one package?**
- **Separation of Concerns** — each layer does one thing
- **Testability** — you can test each layer independently
- **Scalability** — easy to find and modify code as the project grows
- **Clean Architecture** — follows SOLID principles, especially Single Responsibility

---

## Key Files Walkthrough

### `InterviewpilotApplication.java`
```java
@SpringBootApplication
public class InterviewpilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewpilotApplication.class, args);
    }
}
```
- `@SpringBootApplication` is a combo of 3 annotations:
  - `@Configuration` — marks this as a source of bean definitions
  - `@EnableAutoConfiguration` — Spring Boot auto-configures based on dependencies in `pom.xml`
  - `@ComponentScan` — scans this package and sub-packages for Spring components (`@Controller`, `@Service`, etc.)
- `SpringApplication.run()` — boots the embedded Tomcat server and initializes the Spring context

### `HomeController.java`
```java
@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
```
- `@Controller` (not `@RestController`) — tells Spring this returns **view names**, not raw data
- `@GetMapping("/")` — maps HTTP GET requests to "/" to this method
- `return "index"` — Spring looks for `templates/index.html` and renders it via Thymeleaf

**Interview Trap:** "@Controller vs @RestController"
- `@Controller` → returns view names → Thymeleaf resolves them to HTML templates
- `@RestController` = `@Controller` + `@ResponseBody` → returns data directly (JSON/XML)
- If we used `@RestController` here, it would return the text "index" instead of the HTML page!

### `application.yml`
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:interviewpilot    # In-memory database
  jpa:
    hibernate:
      ddl-auto: update                 # Auto-create/update tables from entities
    show-sql: true                     # Log SQL queries (helpful for debugging)
  h2:
    console:
      enabled: true                    # Access H2 console at /h2-console
  thymeleaf:
    cache: false                       # Disable caching for live reload during dev
```

---

## Spring Boot Fundamentals

### What Happens When You Run the App?
1. `main()` calls `SpringApplication.run()`
2. Spring Boot scans for `@SpringBootApplication`
3. Auto-configuration kicks in — detects dependencies (Web, JPA, H2, Thymeleaf) and configures them
4. Embedded Tomcat starts on port 8080
5. Component scan finds `@Controller`, `@Service`, etc. and registers them as Spring beans
6. App is ready to serve requests

### Dependency Injection (DI)
- **What?** Instead of creating objects yourself (`new SomeService()`), Spring creates and manages them for you (as "beans") and injects them where needed.
- **Why?** Loose coupling, easier testing (you can inject mocks), single source of truth for configuration.
- **How?** Use `@Autowired` or better, **constructor injection** (we'll use this in Phase 2).

### Spring Bean Lifecycle
1. Spring scans classpath for annotated classes
2. Creates instances (beans) and stores them in the **Application Context** (IoC Container)
3. Injects dependencies into beans (DI)
4. Beans are ready to use
5. On shutdown, Spring destroys beans and cleans up

---

## Thymeleaf Basics

### How Thymeleaf Works
1. Controller returns a view name (e.g., `"index"`)
2. Thymeleaf resolver looks for `templates/index.html`
3. Thymeleaf processes the template — replaces `th:` attributes with actual data
4. Returns fully rendered HTML to the browser

### Key Thymeleaf Attributes (We'll Use These)
| Attribute | Purpose | Example |
|---|---|---|
| `th:text` | Set text content | `<p th:text="${message}">Default</p>` |
| `th:each` | Loop over a list | `<div th:each="item : ${items}">` |
| `th:if` | Conditional rendering | `<span th:if="${score > 7}">Great!</span>` |
| `th:href` | Dynamic URL | `<a th:href="@{/interview/{id}(id=${session.id})}">` |
| `th:action` | Form submit URL | `<form th:action="@{/submit}">` |
| `th:object` | Bind form to object | `<form th:object="${dto}">` |

### Static Resources
- Files in `src/main/resources/static/` are served directly
- `th:href="@{/css/style.css}"` resolves to `static/css/style.css`
- The `@{...}` syntax handles context paths automatically

---

## Configuration Deep Dive

### `pom.xml` Dependencies Explained
| Dependency | What It Provides |
|---|---|
| `spring-boot-starter-web` | Embedded Tomcat, Spring MVC, REST support |
| `spring-boot-starter-thymeleaf` | Thymeleaf template engine integration |
| `spring-boot-starter-data-jpa` | JPA + Hibernate for database ORM |
| `spring-boot-starter-validation` | Bean validation (`@NotNull`, `@Size`, etc.) |
| `spring-boot-starter-websocket` | WebSocket support (for real-time features later) |
| `h2` (runtime) | H2 in-memory database |
| `spring-boot-starter-test` (test) | JUnit 5, Mockito, Spring Test utilities |

### What is a "Starter"?
A Spring Boot starter is a **curated set of dependencies** bundled together. Instead of manually adding 10 JARs for web development, you add one starter: `spring-boot-starter-web`.

### `ddl-auto: update` Explained
| Value | Behavior |
|---|---|
| `none` | Do nothing — you manage schema manually |
| `validate` | Validate schema matches entities, fail if not |
| `update` | Auto-update schema to match entities (safe for dev) |
| `create` | Drop and recreate schema on every startup |
| `create-drop` | Same as create, but also drop on shutdown |

We use `update` for development — it creates tables from our entities and alters them as we change.

---

## Interview Q&A

### Q: What is Spring Boot and why did you use it?
**A:** Spring Boot is an opinionated framework built on top of Spring Framework. It provides auto-configuration, embedded servers, and starter dependencies to eliminate boilerplate. I used it because it's the industry standard for Java web apps, and it let me focus on business logic instead of configuration.

### Q: Explain the difference between @Controller and @RestController.
**A:** `@Controller` returns view names that are resolved by a template engine (like Thymeleaf). `@RestController` adds `@ResponseBody` to every method, meaning it returns data directly (JSON/XML) without view resolution. I used `@Controller` because my app serves Thymeleaf HTML pages.

### Q: What is the Maven Wrapper and why use it?
**A:** The Maven Wrapper (`mvnw`) is a script that downloads and uses a specific Maven version without requiring Maven to be installed globally. It ensures everyone building the project uses the same Maven version, avoiding "works on my machine" issues.

### Q: Why use YAML over properties for configuration?
**A:** YAML is more readable for hierarchical configuration — it uses indentation instead of repetitive dot notation. `spring.datasource.url` becomes a nested structure in YAML. It also supports lists and multi-line values more naturally.

### Q: Explain Layered Architecture.
**A:** It's a design pattern where the application is divided into layers, each with a specific responsibility: Controller (HTTP handling) → Service (business logic) → Repository (data access) → Model (data representation). Each layer only depends on the layer below it. This gives separation of concerns, testability, and maintainability.

### Q: What is Dependency Injection?
**A:** DI is a design pattern where objects receive their dependencies from an external source (the Spring container) instead of creating them internally. This gives loose coupling — components don't need to know how to create their dependencies. It also makes testing easy because you can inject mock objects.

### Q: What does @SpringBootApplication do?
**A:** It's a convenience annotation combining three annotations: `@Configuration` (source of bean definitions), `@EnableAutoConfiguration` (auto-configures based on classpath), and `@ComponentScan` (scans for Spring components in current and sub-packages).

### Q: What is H2 and why use it?
**A:** H2 is a lightweight, in-memory Java SQL database. I use it for development because it requires zero setup — no installation, no external server. For production, I can switch to PostgreSQL by changing just the configuration, with no code changes needed thanks to JPA's database abstraction.

---

*Next: [Phase 2 — Interview Session Management](PHASE_2_SESSION_MANAGEMENT.md)*
