# 🎯 InterviewPilot — Project Overview

## What is InterviewPilot?

InterviewPilot is a **web application that simulates real technical interviews using AI**. Think of it as a personal interview coach — you select a role (like Software Engineer or Data Scientist), pick a topic (like DSA or System Design), choose a difficulty level, and the app generates interview questions for you in a **real-time chat interface**. You answer them, and the AI evaluates your response with scores and feedback.

---

## 🧱 Tech Stack — Explained Simply

### **Spring Boot** (Backend Framework)
Spring Boot is a **Java framework** that makes it very easy to build production-ready web applications. Instead of writing hundreds of lines of configuration, Spring Boot gives you sensible defaults — you just write your business logic. It handles:
- **Receiving HTTP requests** (like when a user clicks "Start Interview")
- **Connecting to the database** to save/retrieve interview sessions
- **Calling external APIs** (like the AI service)

Think of it as the **brain of the application** — it processes everything behind the scenes.

### **Thymeleaf** (Server-Side Templating)
Thymeleaf is a **template engine** — it generates HTML pages on the server before sending them to the browser. Instead of building a separate frontend app (like React), the HTML is rendered by the backend itself. This keeps the project simpler and tightly integrated.

### **HTMX** (Dynamic Frontend Without Heavy JS)
HTMX is a lightweight JavaScript library that lets you make **AJAX requests directly from HTML attributes** — no need to write JavaScript manually. For example, when a user sends a message in the chat, HTMX sends the request to the server and **swaps just that part of the page** with the response, without a full page reload. This gives it a **single-page app (SPA) feel** without the complexity of React or Angular.

### **Groq API** (AI Engine)
Groq provides a fast inference API for large language models. The app sends **structured prompts** to Groq (e.g., "Generate a medium-difficulty DSA question for an SDE role"), and Groq returns the AI-generated question or answer evaluation. The app parses this response and displays it in the chat.

### **H2 Database** (Storage)
H2 is an **in-memory/embedded database** used during development. It stores interview sessions, questions, and user responses. In production, this can be swapped for PostgreSQL without changing any code, thanks to Spring Data JPA.

### **Spring Data JPA** (Database Layer)
JPA (Java Persistence API) lets you interact with the database **using Java objects instead of writing raw SQL**. You define an `InterviewSession` class, and JPA automatically maps it to a database table. Spring Data JPA further simplifies this — you just define an interface, and it auto-generates the queries.

### **WebSocket** (Real-Time Communication)
WebSocket enables **two-way, real-time communication** between the browser and server. This powers the live chat interface where questions and answers flow back and forth instantly.

---

## 🏗️ How It All Fits Together (The Flow)

```
User opens browser → Thymeleaf renders the UI
       ↓
User creates an interview session (role, topic, difficulty)
       ↓
Spring Boot Controller receives the request → saves session to H2 via JPA
       ↓
User starts the interview → HTMX sends request without page reload
       ↓
InterviewService calls Groq API with a structured prompt
       ↓
AI generates a question → sent back to browser via WebSocket/HTMX
       ↓
User answers → AI evaluates → score & feedback displayed
```

---

## 🗣️ One-Liner Pitch

> *"InterviewPilot is a full-stack Java web app that uses AI to simulate technical interviews — it generates role-specific questions, lets you answer in a real-time chat, and gives instant AI-powered feedback and scoring."*
