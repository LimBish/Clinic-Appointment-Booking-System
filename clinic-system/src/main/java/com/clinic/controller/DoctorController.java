package com.clinic.controller;

import com.clinic.service.AppointmentService;
import com.clinic.service.DoctorService;
import com.clinic.service.QueueService;
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
 * DoctorController — Presentation Layer for the DOCTOR role.
 *
 * Paths:
 * /doctor/dashboard — today's schedule overview
 * /doctor/schedule — weekly schedule view
 * /doctor/schedule?date=YYYY-MM-DD — daily schedule for a date
 * /doctor/queue — live queue management panel
 * /doctor/queue/next — call next patient
 * /doctor/appointments/{id}/complete — mark appointment completed
 */
@Controller
@RequestMapping("/doctor")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorController {

    private final AppointmentService appointmentService;
    private final QueueService queueService;
    private final DoctorService doctorService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long doctorId = doctorService.getDoctorIdByUserId(userId);
        LocalDate today = LocalDate.now();

        model.addAttribute("todayAppointments",
                appointmentService.getDoctorDailySchedule(doctorId, today));
        model.addAttribute("queueEntries",
                queueService.getDoctorQueue(doctorId, today));
        model.addAttribute("today", today);
        return "doctor/dashboard";
    }

    @GetMapping("/schedule")
    public String schedule(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        Long userId = SecurityUtils.getCurrentUserId();
        Long doctorId = doctorService.getDoctorIdByUserId(userId);
        LocalDate targetDate = (date != null) ? date : LocalDate.now();

        model.addAttribute("appointments",
                appointmentService.getDoctorDailySchedule(doctorId, targetDate));
        model.addAttribute("weekAppointments",
                appointmentService.getDoctorWeeklySchedule(doctorId,
                        targetDate.with(java.time.DayOfWeek.MONDAY)));
        model.addAttribute("selectedDate", targetDate);
        return "doctor/schedule";
    }

    @GetMapping("/queue")
    public String queuePanel(Model model) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long doctorId = doctorService.getDoctorIdByUserId(userId);

        model.addAttribute("queue",
                queueService.getDoctorQueue(doctorId, LocalDate.now()));
        model.addAttribute("doctorId", doctorId);
        return "doctor/queue";
    }

    @PostMapping("/queue/next")
    public String callNext(RedirectAttributes redirectAttributes) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long doctorId = doctorService.getDoctorIdByUserId(userId);
        try {
            queueService.callNextPatient(doctorId);
            redirectAttributes.addFlashAttribute("successMsg", "Next patient called.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/doctor/queue";
    }

    @PostMapping("/appointments/{id}/complete")
    public String completeAppointment(@PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            appointmentService.completeAppointment(id, SecurityUtils.getCurrentUserId());
            redirectAttributes.addFlashAttribute("successMsg", "Appointment marked as completed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/doctor/queue";
    }
}
