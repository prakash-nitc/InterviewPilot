package com.prakash.interviewpilot.controller;

import org.springframework.stereotype.Controller;
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

    /**
     * Serves the landing page at the root URL "/".
     *
     * @return the name of the Thymeleaf template (maps to templates/index.html)
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }
}
