package com.clinic.controller;

import com.clinic.dto.request.PatientRegistrationRequest;
import com.clinic.service.ClinicService;
import com.clinic.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * AuthController — Presentation Layer for public authentication pages.
 *
 * Paths:
 *   GET  /auth/login     → renders login form
 *   GET  /auth/register  → renders patient registration form
 *   POST /auth/register  → processes registration
 *
 * After login, Spring Security handles the /auth/login POST automatically.
 * On success, RoleBasedLoginSuccessHandler redirects to the correct dashboard.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClinicService clinicService;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            Model model) {

        if (error != null) model.addAttribute("errorMsg", "Invalid email or password.");
        if (logout != null) model.addAttribute("successMsg", "You have been logged out.");
        if (expired != null) model.addAttribute("errorMsg", "Session expired. Please log in again.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new PatientRegistrationRequest());
        model.addAttribute("activeClinics", clinicService.getClinicsByStatus("ACTIVE"));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationRequest") PatientRegistrationRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("activeClinics", clinicService.getClinicsByStatus("ACTIVE"));
            return "auth/register";
        }
        try {
            userService.registerPatient(request);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Registration successful! Please log in.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/auth/register";
        }
    }
}