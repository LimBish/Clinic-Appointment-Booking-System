package com.clinic.controller;

import com.clinic.dto.request.ClinicAdminCreateRequest;
import com.clinic.dto.request.ClinicCreateRequest;
import com.clinic.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * SuperAdminController — Presentation Layer for the SUPER_ADMIN role.
 *
 * SUPER_ADMIN is the platform owner. They:
 *  - See ALL clinics, their statuses and metrics
 *  - Create new clinics and assign their admins
 *  - Activate / suspend / deactivate clinics
 *  - View platform-wide statistics
 *  - Adjust per-clinic resource limits (maxDoctors, subscription plan)
 *
 * All endpoints are restricted to ROLE_SUPER_ADMIN at both URL level
 * (SecurityConfig) and method level (@PreAuthorize).
 *
 * URL namespace: /superadmin/** — completely separate from /admin/**
 * This prevents any possibility of URL-level privilege escalation.
 *
 * Paths:
 *   GET  /superadmin/dashboard              — platform overview with stats
 *   GET  /superadmin/clinics                — all clinics table
 *   GET  /superadmin/clinics/new            — create clinic form
 *   POST /superadmin/clinics                — save new clinic
 *   GET  /superadmin/clinics/{id}           — clinic detail / management
 *   GET  /superadmin/clinics/{id}/edit      — edit clinic config
 *   POST /superadmin/clinics/{id}           — update clinic config
 *   POST /superadmin/clinics/{id}/activate  — activate clinic
 *   POST /superadmin/clinics/{id}/suspend   — suspend clinic
 *   POST /superadmin/clinics/{id}/deactivate— permanently close clinic
 *   GET  /superadmin/clinics/{id}/admins/new— create admin form
 *   POST /superadmin/clinics/{id}/admins    — assign admin to clinic
 */
@Controller
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class SuperAdminController {

    private final ClinicService clinicService;

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("stats", clinicService.getPlatformStats());
        model.addAttribute("recentClinics", clinicService.getAllClinics());
        return "superadmin/dashboard";
    }

    // ─── Clinic List ──────────────────────────────────────────────────────────

    @GetMapping("/clinics")
    public String clinicList(
            @RequestParam(required = false) String status,
            Model model) {

        if (status != null && !status.isBlank()) {
            model.addAttribute("clinics", clinicService.getClinicsByStatus(status));
            model.addAttribute("filterStatus", status);
        } else {
            model.addAttribute("clinics", clinicService.getAllClinics());
        }
        return "superadmin/clinics";
    }

    // ─── Create Clinic ────────────────────────────────────────────────────────

    @GetMapping("/clinics/new")
    public String newClinicForm(Model model) {
        model.addAttribute("clinicRequest", new ClinicCreateRequest());
        return "superadmin/clinic-form";
    }

    @PostMapping("/clinics")
    public String createClinic(
            @Valid @ModelAttribute("clinicRequest") ClinicCreateRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            return "superadmin/clinic-form";
        }
        try {
            clinicService.createClinic(request);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Clinic '" + request.getName() + "' created successfully. Status: PENDING.");
            return "redirect:/superadmin/clinics";
        } catch (Exception e) {
            model.addAttribute("errorMsg", e.getMessage());
            return "superadmin/clinic-form";
        }
    }

    // ─── Clinic Detail ────────────────────────────────────────────────────────

    @GetMapping("/clinics/{id}")
    public String clinicDetail(@PathVariable Long id, Model model) {
        model.addAttribute("clinic", clinicService.getClinicById(id));
        model.addAttribute("adminRequest", new ClinicAdminCreateRequest());
        return "superadmin/clinic-detail";
    }

    // ─── Edit Clinic Config ───────────────────────────────────────────────────

    @GetMapping("/clinics/{id}/edit")
    public String editClinicForm(@PathVariable Long id, Model model) {
        model.addAttribute("clinic", clinicService.getClinicById(id));
        model.addAttribute("clinicRequest", new ClinicCreateRequest());
        return "superadmin/clinic-edit";
    }

    @PostMapping("/clinics/{id}/edit")
    public String updateClinic(@PathVariable Long id,
                                @Valid @ModelAttribute("clinicRequest") ClinicCreateRequest request,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "superadmin/clinic-edit";
        try {
            clinicService.updateClinicConfig(id, request);
            redirectAttributes.addFlashAttribute("successMsg", "Clinic configuration updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/superadmin/clinics/" + id;
    }

    // ─── Status Actions ───────────────────────────────────────────────────────

    @PostMapping("/clinics/{id}/activate")
    public String activateClinic(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes) {
        try {
            clinicService.activateClinic(id);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Clinic activated. All clinic users can now log in.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/superadmin/clinics/" + id;
    }

    @PostMapping("/clinics/{id}/suspend")
    public String suspendClinic(@PathVariable Long id,
                                 @RequestParam String reason,
                                 RedirectAttributes redirectAttributes) {
        try {
            clinicService.suspendClinic(id, reason);
            redirectAttributes.addFlashAttribute("warningMsg",
                    "Clinic suspended. All clinic users are blocked immediately.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/superadmin/clinics/" + id;
    }

    @PostMapping("/clinics/{id}/deactivate")
    public String deactivateClinic(@PathVariable Long id,
                                    @RequestParam String reason,
                                    RedirectAttributes redirectAttributes) {
        try {
            clinicService.deactivateClinic(id, reason);
            redirectAttributes.addFlashAttribute("warningMsg",
                    "Clinic permanently deactivated. Data retained for audit.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/superadmin/clinics/" + id;
    }

    // ─── Assign Admin ─────────────────────────────────────────────────────────

    @GetMapping("/clinics/{id}/admins/new")
    public String newAdminForm(@PathVariable Long id, Model model) {
        model.addAttribute("clinic", clinicService.getClinicById(id));
        model.addAttribute("adminRequest", new ClinicAdminCreateRequest());
        return "superadmin/clinic-admin-form";
    }

    @PostMapping("/clinics/{id}/admins")
    public String assignAdmin(@PathVariable Long id,
                               @Valid @ModelAttribute("adminRequest") ClinicAdminCreateRequest request,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("clinic", clinicService.getClinicById(id));
            return "superadmin/clinic-admin-form";
        }
        try {
            clinicService.assignClinicAdmin(id, request);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Admin account '" + request.getEmail() + "' created and assigned.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/superadmin/clinics/" + id;
    }
}
