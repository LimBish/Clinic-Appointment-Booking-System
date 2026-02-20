package com.clinic.controller;

import com.clinic.dto.request.AppointmentBookRequest;
import com.clinic.dto.request.AppointmentRescheduleRequest;
import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.QueueService;
import com.clinic.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * PatientController — Presentation Layer for the PATIENT role.
 *
 * All endpoints require ROLE_PATIENT (enforced at SecurityConfig level + @PreAuthorize).
 *
 * Paths:
 *   /patient/dashboard              — upcoming appointments + queue status
 *   /patient/appointments           — full history
 *   /patient/appointments/book      — booking form
 *   /patient/appointments/{id}      — view detail
 *   /patient/appointments/{id}/reschedule  — reschedule form
 *   /patient/appointments/{id}/cancel      — cancel
 *   /patient/queue                  — real-time queue position
 *   /patient/doctors                — browse available doctors
 */
@Controller
@RequestMapping("/patient")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientController {

    private final AppointmentService appointmentService;
    private final DoctorService doctorService;
    private final QueueService queueService;

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        model.addAttribute("upcomingAppointments",
                appointmentService.getUpcomingAppointments(userId));
        // Try to load today's queue status — may be empty if not checked in
        try {
            model.addAttribute("queueStatus",
                    queueService.getMyQueueStatus(userId, LocalDate.now()));
        } catch (Exception ignored) {
            model.addAttribute("queueStatus", null);
        }
        return "patient/dashboard";
    }

    // ─── Appointment List ─────────────────────────────────────────────────────

    @GetMapping("/appointments")
    public String appointmentHistory(Model model) {
        model.addAttribute("appointments",
                appointmentService.getPatientAppointments(SecurityUtils.getCurrentUserId()));
        return "patient/appointments";
    }

    // ─── Book Appointment ─────────────────────────────────────────────────────

    @GetMapping("/appointments/book")
    public String bookForm(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        model.addAttribute("doctors", doctorService.getAllAvailableDoctors());
        model.addAttribute("specializations", doctorService.getAllSpecializations());
        model.addAttribute("bookRequest", new AppointmentBookRequest());

        if (doctorId != null && date != null) {
            model.addAttribute("selectedDoctor", doctorService.getDoctorById(doctorId));
            model.addAttribute("availableSlots",
                    appointmentService.getAvailableSlots(doctorId, date));
            model.addAttribute("selectedDate", date);
        }
        return "patient/book-appointment";
    }

    @PostMapping("/appointments/book")
    public String book(@Valid @ModelAttribute("bookRequest") AppointmentBookRequest request,
                       BindingResult result,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        if (result.hasErrors()) {
            model.addAttribute("doctors", doctorService.getAllAvailableDoctors());
            return "patient/book-appointment";
        }
        try {
            appointmentService.bookAppointment(SecurityUtils.getCurrentUserId(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Appointment booked successfully!");
            return "redirect:/patient/appointments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/patient/appointments/book";
        }
    }

    // ─── Appointment Detail ───────────────────────────────────────────────────

    @GetMapping("/appointments/{id}")
    public String appointmentDetail(@PathVariable Long id, Model model) {
        model.addAttribute("appointment",
                appointmentService.getAppointmentById(id, SecurityUtils.getCurrentUserId()));
        return "patient/appointment-detail";
    }

    // ─── Reschedule ───────────────────────────────────────────────────────────

    @GetMapping("/appointments/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id, Model model) {
        model.addAttribute("appointment",
                appointmentService.getAppointmentById(id, SecurityUtils.getCurrentUserId()));
        model.addAttribute("rescheduleRequest", new AppointmentRescheduleRequest());
        return "patient/reschedule";
    }

    @PostMapping("/appointments/{id}/reschedule")
    public String reschedule(@PathVariable Long id,
                             @Valid @ModelAttribute("rescheduleRequest")
                             AppointmentRescheduleRequest request,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) return "patient/reschedule";
        try {
            appointmentService.rescheduleAppointment(id, SecurityUtils.getCurrentUserId(), request);
            redirectAttributes.addFlashAttribute("successMsg", "Appointment rescheduled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    // ─── Cancel ──────────────────────────────────────────────────────────────

    @PostMapping("/appointments/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @RequestParam(defaultValue = "Cancelled by patient") String reason,
                         RedirectAttributes redirectAttributes) {
        try {
            appointmentService.cancelAppointment(id, SecurityUtils.getCurrentUserId(), reason);
            redirectAttributes.addFlashAttribute("successMsg", "Appointment cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/patient/appointments";
    }

    // ─── Queue Status ─────────────────────────────────────────────────────────

    /**
     * Real-time queue position page.
     * Thymeleaf template uses <meta http-equiv="refresh" content="30">
     * to auto-refresh every 30 seconds without JavaScript.
     */
    @GetMapping("/queue")
    public String queueStatus(Model model) {
        try {
            model.addAttribute("queueEntry",
                    queueService.getMyQueueStatus(SecurityUtils.getCurrentUserId(), LocalDate.now()));
        } catch (Exception e) {
            model.addAttribute("notInQueue", true);
        }
        return "patient/queue-status";
    }

    // ─── Browse Doctors ───────────────────────────────────────────────────────

    @GetMapping("/doctors")
    public String browseDoctors(
            @RequestParam(required = false) String specialization,
            Model model) {
        model.addAttribute("specializations", doctorService.getAllSpecializations());
        if (specialization != null && !specialization.isBlank()) {
            model.addAttribute("doctors", doctorService.getDoctorsBySpecialization(specialization));
            model.addAttribute("selectedSpec", specialization);
        } else {
            model.addAttribute("doctors", doctorService.getAllAvailableDoctors());
        }
        return "patient/doctors";
    }
}
