package com.prakash.interviewpilot.controller;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.*;
import com.prakash.interviewpilot.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
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

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
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
     */
    @PostMapping("/new")
    public String createSession(
            @Valid @ModelAttribute("sessionRequest") CreateSessionRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", InterviewRole.values());
            model.addAttribute("topics", InterviewTopic.values());
            model.addAttribute("difficulties", Difficulty.values());
            return "create-session";
        }

        InterviewSession session = interviewService.createSession(request);
        redirectAttributes.addFlashAttribute("successMessage", "Interview session created successfully!");
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
        model.addAttribute("session", session);
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

        model.addAttribute("session", session);
        model.addAttribute("currentQuestion", currentQuestion);
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

        model.addAttribute("session", session);

        if (nextQuestion != null) {
            model.addAttribute("question", nextQuestion);
            return "fragments/question-card :: questionCard(session=${session}, question=${question})";
        } else {
            return "fragments/question-card :: allDone(session=${session})";
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
