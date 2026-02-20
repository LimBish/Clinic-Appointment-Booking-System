package com.clinic.scheduler;

import com.clinic.model.entity.Appointment;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AppointmentReminderScheduler — sends 24-hour reminders automatically.
 *
 * Sprint 4 feature: automated appointment reminders.
 *
 * Runs every hour using a cron expression.
 * The window [now+23h, now+25h] catches all appointments due in ~24 hours
 * regardless of exactly when the scheduler runs within the hour.
 *
 * reminderSent flag prevents duplicate sends on scheduler re-runs.
 *
 * Also handles NO_SHOW detection: CONFIRMED appointments from past days
 * that were never checked in are marked as NO_SHOW.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Value("${clinic.reminder.hours-before:24}")
    private int hoursBeforeReminder;

    /**
     * Runs every hour at minute 0.
     * cron = "0 0 * * * *" → second=0, minute=0, every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendReminders() {
        log.info("ReminderScheduler: checking for upcoming appointments...");

        LocalDateTime windowStart = LocalDateTime.now().plusHours(hoursBeforeReminder - 1);
        LocalDateTime windowEnd   = LocalDateTime.now().plusHours(hoursBeforeReminder + 1);

        List<Appointment> candidates = appointmentRepository
                .findReminderCandidates(windowStart, windowEnd);

        log.info("ReminderScheduler: found {} candidates", candidates.size());

        for (Appointment appointment : candidates) {
            try {
                notificationService.sendAppointmentReminder(appointment);
                appointment.setReminderSent(true);
                appointmentRepository.save(appointment);
            } catch (Exception e) {
                log.error("Failed to send reminder for appointment {}: {}",
                        appointment.getId(), e.getMessage());
                // Continue with other reminders — don't let one failure block the rest
            }
        }
    }

    /**
     * Runs once per day at 01:00 AM.
     * Marks any appointments from yesterday that are still CONFIRMED as NO_SHOW.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void markNoShows() {
        log.info("ReminderScheduler: marking no-shows from previous days...");

        List<Appointment> overdue = appointmentRepository.findOverdueConfirmed(LocalDate.now());
        overdue.forEach(a -> {
            a.setStatus(AppointmentStatus.NO_SHOW);
            appointmentRepository.save(a);
            log.info("Appointment {} marked as NO_SHOW", a.getId());
        });
    }
}
