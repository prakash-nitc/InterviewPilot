package com.prakash.interviewpilot.controller;

import com.prakash.interviewpilot.dto.CreateSessionRequest;
import com.prakash.interviewpilot.model.Difficulty;
import com.prakash.interviewpilot.model.InterviewRole;
import com.prakash.interviewpilot.model.InterviewSession;
import com.prakash.interviewpilot.model.InterviewTopic;
import com.prakash.interviewpilot.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
     *
     * WHY add enum values to the Model?
     * - Thymeleaf needs the enum values to populate dropdown options.
     * - Model is a key-value map that Thymeleaf can access in templates.
     *
     * WHY add an empty CreateSessionRequest?
     * - Thymeleaf form binding (th:object) needs an object to bind to.
     * - On form submission, Spring populates this object with form data.
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
     *
     * WHY @Valid + BindingResult?
     * - @Valid triggers validation of @NotNull etc. on the DTO.
     * - BindingResult captures validation errors.
     * - If there are errors, we re-display the form with error messages.
     * - BindingResult MUST come immediately after the @Valid parameter.
     *
     * WHY RedirectAttributes?
     * - After successful creation, we redirect to the session detail page.
     * - RedirectAttributes lets us pass a flash message ("Session created!")
     * that survives the redirect.
     *
     * WHY "redirect:" instead of returning a view name?
     * - POST-Redirect-GET pattern: prevents double form submission if user
     * refreshes the browser after submitting.
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
     *
     * WHY @PathVariable?
     * - Extracts the {id} from the URL path (e.g., /interviews/5 → id = 5).
     * - This is RESTful URL design — resources identified by their ID in the URL.
     */
    @GetMapping("/{id}")
    public String sessionDetail(@PathVariable Long id, Model model) {
        InterviewSession session = interviewService.getSession(id);
        model.addAttribute("session", session);
        return "session-detail";
    }

    /**
     * Starts an interview session (generates AI questions).
     *
     * WHY @PostMapping?
     * - Starting a session modifies state (changes status, adds questions).
     * - GET requests should be idempotent (safe to refresh). POST is for actions.
     */
    @PostMapping("/{id}/start")
    public String startSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            interviewService.startSession(id);
            redirectAttributes.addFlashAttribute("successMessage", "Interview started! Questions generated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to start interview: " + e.getMessage());
        }
        return "redirect:/interviews/" + id;
    }

    /**
     * Deletes a session and redirects back to the session list.
     *
     * WHY @PostMapping (not @DeleteMapping)?
     * - HTML forms only support GET and POST methods.
     * - We use POST for delete actions in a Thymeleaf app.
     * - In a REST API, we'd use @DeleteMapping.
     */
    @PostMapping("/{id}/delete")
    public String deleteSession(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        interviewService.deleteSession(id);
        redirectAttributes.addFlashAttribute("successMessage", "Session deleted successfully!");
        return "redirect:/interviews";
    }
}
