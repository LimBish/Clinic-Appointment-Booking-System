package com.clinic.controller;

import com.clinic.repository.UserRepository;
import com.clinic.service.*;
import com.clinic.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * AdminController — Presentation Layer for the ADMIN role.
 *
 * Admin capabilities (from Week 2 requirements):
 *   - Manage doctor profiles and schedules
 *   - Monitor patient queues
 *   - Generate daily/range reports
 *   - Resolve scheduling disputes
 *   - Suspend/enable user accounts
 *
 * Paths:
 *   /admin/dashboard           — system overview metrics
 *   /admin/doctors             — doctor management
 *   /admin/doctors/{id}        — doctor detail / schedule
 *   /admin/queue               — live all-doctor queue view
 *   /admin/queue/{doctorId}    — specific doctor queue
 *   /admin/appointments        — all appointments with filters
 *   /admin/users               — user management
 *   /admin/reports             — daily/range reports
 */
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final DoctorService doctorService;
    private final UserService userService;
    private final AdminReportService reportService;
    private final ClinicService clinicService;
    private final UserRepository userRepository;



    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("dailyReport", reportService.getDailyReport(today));
        model.addAttribute("allDoctors", doctorService.getAllDoctors());
        model.addAttribute("today", today);
        return "admin/dashboard";
    }

    // ─── Doctor Management ────────────────────────────────────────────────────

    @GetMapping("/doctors")
    public String doctorList(Model model) {
        model.addAttribute("doctors", doctorService.getAllDoctors());
        return "admin/doctors";
    }

    @GetMapping("/doctors/{id}")
    public String doctorDetail(@PathVariable Long id,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                Model model) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("doctor", doctorService.getDoctorById(id));
        model.addAttribute("schedule", appointmentService.getDoctorDailySchedule(id, targetDate));
        model.addAttribute("selectedDate", targetDate);
        return "admin/doctor-detail";
    }

    // ─── Queue Management ─────────────────────────────────────────────────────

    @GetMapping("/queue")
    public String queueOverview(Model model) {
        model.addAttribute("allDoctors", doctorService.getAllAvailableDoctors());
        return "admin/queue-overview";
    }

    @GetMapping("/queue/{doctorId}")
    public String doctorQueue(@PathVariable Long doctorId,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("queue", queueService.getDoctorQueue(doctorId, targetDate));
        model.addAttribute("doctor", doctorService.getDoctorById(doctorId));
        model.addAttribute("selectedDate", targetDate);
        return "admin/queue-detail";
    }

    @PostMapping("/queue/{entryId}/skip")
    public String skipPatient(@PathVariable Long entryId,
                               @RequestParam Long doctorId,
                               RedirectAttributes redirectAttributes) {
        try {
            queueService.skipPatient(entryId);
            redirectAttributes.addFlashAttribute("successMsg", "Patient skipped.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/queue/" + doctorId;
    }

    // ─── Walk-In Registration ─────────────────────────────────────────────────

    @PostMapping("/queue/{doctorId}/walkin")
    public String addWalkIn(@PathVariable Long doctorId,
                             @RequestParam Long patientUserId,
                             RedirectAttributes redirectAttributes) {
        try {
            queueService.addWalkIn(doctorId, patientUserId);
            redirectAttributes.addFlashAttribute("successMsg", "Walk-in patient added to queue.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/queue/" + doctorId;
    }

    // ─── Appointments ─────────────────────────────────────────────────────────

//    @GetMapping("/appointments")
//    public String appointments(
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
//            @RequestParam(required = false)
//            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
//            Model model) {
//        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(7);
//        LocalDate end   = (to   != null) ? to   : LocalDate.now();
//        model.addAttribute("appointments", appointmentService.getAppointmentsByDateRange(start, end));
//        model.addAttribute("from", start);
//        model.addAttribute("to", end);
//        return "admin/appointments";
//    }

    @GetMapping("/appointments")
    public String appointments(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {

        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(7);
        LocalDate end   = (to   != null) ? to   : LocalDate.now();

        Long currentUserId = SecurityUtils.getCurrentUserId();

        // Assuming ADMIN belongs to a clinic
        Long clinicId = userService.getClinicIdByUserId(currentUserId);

        model.addAttribute("appointments",
                appointmentService.getAppointmentsByDateRange(clinicId, start, end));

        model.addAttribute("from", start);
        model.addAttribute("to", end);

        return "admin/appointments";
    }


    @PostMapping("/appointments/{id}/cancel")
    public String adminCancelAppointment(@PathVariable Long id,
                                          @RequestParam String reason,
                                          RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(id, SecurityUtils.getCurrentUserId(), reason);
            redirectAttributes.addFlashAttribute("successMsg", "Appointment cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/appointments";
    }

    // ─── User Management ─────────────────────────────────────────────────────

    @GetMapping("/users")
    public String userList(Model model) {
        model.addAttribute("patients", doctorService.getAllDoctors()); // placeholder
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserEnabled(id);
            redirectAttributes.addFlashAttribute("successMsg", "User status updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ─── Reports ─────────────────────────────────────────────────────────────

    @GetMapping("/reports")
    public String reports(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Model model) {
        LocalDate start = (from != null) ? from : LocalDate.now().minusDays(30);
        LocalDate end   = (to   != null) ? to   : LocalDate.now();
        model.addAttribute("report", reportService.getDateRangeReport(start, end));
        model.addAttribute("from", start);
        model.addAttribute("to", end);
        return "admin/reports";
    }
}
