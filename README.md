# 🎯 InterviewPilot

**AI-Powered Mock Interview Platform** — Practice technical interviews with an AI interviewer, get real-time scoring and feedback, and track your improvement over time.

## 🚀 What is InterviewPilot?

InterviewPilot is a full-stack web application that simulates real technical interviews:

1. **Choose** your target role (SDE, Data Scientist, etc.), topic (DSA, System Design, Java), and difficulty
2. **Answer** AI-generated interview questions in a natural chat interface
3. **Get scored** instantly with detailed feedback — strengths, improvements, and model answers
4. **Track** your progress across multiple interview sessions with analytics

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.5 |
| **AI Engine** | Google Gemini API |
| **Database** | H2 (dev) / PostgreSQL (prod) |
| **Frontend** | Thymeleaf + HTMX |
| **Build** | Maven |

## 🏗️ Architecture

```
                    ┌──────────────────────┐
                    │  Thymeleaf + HTMX UI │
                    └──────────┬───────────┘
                               │ HTTP
                    ┌──────────▼───────────┐
                    │  InterviewController  │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │   InterviewService    │
                    ├──────────┬───────────┤
               ┌────▼────┐  ┌─▼──────────┐
               │AIService│  │ScoringService│
               └────┬────┘  └─┬──────────┘
                    │          │
               ┌────▼──────────▼───┐
               │  Google Gemini API │
               └───────────────────┘
                    │
               ┌────▼────────────┐
               │ H2 / PostgreSQL │
               └─────────────────┘
```

## 📋 Project Phases

- [x] **Phase 1:** Project Setup & Foundation
- [x] **Phase 2:** Interview Session Management
- [x] **Phase 3:** AI Integration — Question Generation
- [x] **Phase 4:** Real-Time Chat Interface
- [ ] **Phase 5:** AI-Powered Answer Evaluation & Scoring
- [ ] **Phase 6:** Interview History & Analytics
- [ ] **Phase 7:** Advanced Features
- [ ] **Phase 8:** Deployment & Polish

## 📂 Project Structure

```
src/main/java/com/prakash/interviewpilot/
├── config/          # Configuration classes
├── controller/      # HTTP controllers (Thymeleaf views)
├── dto/             # Data Transfer Objects
├── model/           # JPA entities
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic layer
└── InterviewpilotApplication.java  # Main entry point
```

## 🚀 Running Locally

```bash
# Clone the repo
git clone https://github.com/<your-username>/InterviewPilot.git
cd InterviewPilot

# Run the app (no Maven install needed — uses Maven Wrapper)
./mvnw spring-boot:run      # Linux/Mac
mvnw.cmd spring-boot:run    # Windows

# Visit http://localhost:8080
```

## 📖 Documentation

Detailed phase-by-phase documentation is available in the [`docs/`](docs/) folder:

- [Phase 1: Project Setup](docs/PHASE_1_PROJECT_SETUP.md)
- [Phase 2: Session Management](docs/PHASE_2_SESSION_MANAGEMENT.md)
- [Phase 3: AI Integration](docs/PHASE_3_AI_INTEGRATION.md)
- [Phase 4: Chat Interface](docs/PHASE_4_CHAT_INTERFACE.md)

## 📄 License

This project is for educational and portfolio purposes.
