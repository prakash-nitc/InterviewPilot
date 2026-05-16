package com.prakash.interviewpilot.controller;

import com.prakash.interviewpilot.dto.DashboardStats;
import com.prakash.interviewpilot.service.InterviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController - Serves the landing page of InterviewPilot.
 *
 * WHY @Controller instead of @RestController?
 * - @Controller returns VIEW NAMES (Thymeleaf templates), not raw data.
 * - @RestController would return the string "index" as plain text.
 * - We want Spring to resolve "index" → templates/index.html via Thymeleaf.
 */
@Controller
public class HomeController {

    private final InterviewService interviewService;

    public HomeController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    /**
     * Serves the landing page at the root URL "/".
     * Passes live dashboard stats for the hero section.
     *
     * @return the name of the Thymeleaf template (maps to templates/index.html)
     */
    @GetMapping("/")
    public String home(Model model) {
        DashboardStats stats = interviewService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "index";
    }
}
