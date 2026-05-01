package com.prakash.interviewpilot.controller;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.*;
import com.prakash.interviewpilot.service.InterviewService;
import com.prakash.interviewpilot.service.ResumeParserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller for interview session management.
 *
 * WHY @Controller (not @RestController)?
 * - We're returning Thymeleaf view names, not JSON data.
 *
 * WHY inject InterviewService (not the Repository directly)?
 * - Controller should be "thin" — only handle HTTP concerns.
 * - Business logic (validation, state transitions) belongs in the service.
 * - This is the Layered Architecture pattern in action.
 */
@Controller
@RequestMapping("/interviews")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;
    private final ResumeParserService resumeParserService;

    public InterviewController(InterviewService interviewService, ResumeParserService resumeParserService) {
        this.interviewService = interviewService;
        this.resumeParserService = resumeParserService;
    }

    /**
     * Shows the "Create Interview" form.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("sessionRequest", new CreateSessionRequest());
        model.addAttribute("roles", InterviewRole.values());
        model.addAttribute("topics", InterviewTopic.values());
        model.addAttribute("difficulties", Difficulty.values());
        return "create-session";
    }

    /**
     * Handles the "Create Interview" form submission.
     * Uses POST-Redirect-GET pattern to prevent double submission.
     *
     * RESUME-AWARE INTERVIEWS:
     * If a PDF resume is uploaded, we extract the text and store it
     * on the session. The AI will use this text to generate personalized
     * questions when the session is started.
     */
    @PostMapping("/new")
    public String createSession(
            @Valid @ModelAttribute("sessionRequest") CreateSessionRequest request,
            BindingResult bindingResult,
            @RequestParam(required = false) MultipartFile resume,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", InterviewRole.values());
            model.addAttribute("topics", InterviewTopic.values());
            model.addAttribute("difficulties", Difficulty.values());
            return "create-session";
        }

        InterviewSession session = interviewService.createSession(request);

        // Handle resume upload (optional)
        if (resume != null && !resume.isEmpty()) {
            try {
                String resumeText = resumeParserService.extractText(resume);
                session.setResumeText(resumeText);
                // Save again with resume text
                interviewService.saveSession(session);
                log.info("Resume uploaded and parsed for session {} ({} chars)",
                        session.getId(), resumeText.length());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Interview session created with resume! AI will personalize your questions.");
            } catch (Exception e) {
                log.error("Failed to parse resume, continuing without it", e);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Interview session created! (Resume could not be processed: " + e.getMessage() + ")");
            }
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Interview session created successfully!");
        }

        return "redirect:/interviews/" + session.getId();
    }

    /**
     * Lists all interview sessions.
     */
    @GetMapping
    public String listSessions(Model model) {
        model.addAttribute("sessions", interviewService.getAllSessions());
        return "sessions";
    }

    /**
     * Shows details of a specific interview session.
     */
    @GetMapping("/{id}")
    public String sessionDetail(@PathVariable Long id, Model model) {
        InterviewSession session = interviewService.getSession(id);
        model.addAttribute("interviewSession", session);
        return "session-detail";
    }

    /**
     * Starts an interview session (generates AI questions) then redirects to play page.
     */
    @PostMapping("/{id}/start")
    public String startSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            interviewService.startSession(id);
            return "redirect:/interviews/" + id + "/play";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to start interview: " + e.getMessage());
            return "redirect:/interviews/" + id;
        }
    }

    /**
     * Renders the interview play page (chat-style interface).
     *
     * WHY a separate /play route instead of reusing /session-detail?
     * - Different UI: session-detail is an overview, /play is the interactive chat.
     * - Separation of concerns: different templates for different user intents.
     */
    @GetMapping("/{id}/play")
    public String playInterview(@PathVariable Long id, Model model) {
        InterviewSession session = interviewService.getSession(id);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return "redirect:/interviews/" + id;
        }

        Question currentQuestion = interviewService.getCurrentQuestion(id);
        List<Question> answeredQuestions = interviewService.getAnsweredQuestions(id);

        int currentIndex = answeredQuestions.size() + 1;

        model.addAttribute("interviewSession", session);
        model.addAttribute("currentQuestion", currentQuestion);
        model.addAttribute("question", currentQuestion);  // Fragment uses ${question}
        model.addAttribute("answeredQuestions", answeredQuestions);
        model.addAttribute("currentQuestionIndex", currentIndex);

        return "interview";
    }

    /**
     * Handles answer submission via HTMX.
     *
     * WHY return a fragment instead of a full page?
     * - HTMX expects an HTML fragment to swap into #question-area.
     * - This is the key to "no full page reload" — only the question card updates.
     * - We return either the next question card or the "all done" card.
     */
    @PostMapping("/{id}/answer")
    public String submitAnswer(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @RequestParam String answer,
            Model model) {

        // Save the answer
        interviewService.submitAnswer(id, questionId, answer);

        // Get the next question (or null if all done)
        InterviewSession session = interviewService.getSession(id);
        Question nextQuestion = interviewService.getCurrentQuestion(id);

        model.addAttribute("interviewSession", session);

        if (nextQuestion != null) {
            model.addAttribute("question", nextQuestion);
            return "fragments/question-card :: questionCard";
        } else {
            return "fragments/question-card :: allDone";
        }
    }

    /**
     * Completes an interview session — calculates scores and redirects to detail page.
     */
    @PostMapping("/{id}/complete")
    public String completeSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            interviewService.completeSession(id);
            redirectAttributes.addFlashAttribute("successMessage", "Interview completed! Review your results below.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete interview: " + e.getMessage());
        }
        return "redirect:/interviews/" + id;
    }

    /**
     * Deletes a session and redirects back to the session list.
     */
    @PostMapping("/{id}/delete")
    public String deleteSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        interviewService.deleteSession(id);
        redirectAttributes.addFlashAttribute("successMessage", "Session deleted successfully!");
        return "redirect:/interviews";
    }
}
