package com.clinic.service.impl;

import com.clinic.model.entity.Appointment;
import com.clinic.model.entity.Notification;
import com.clinic.model.entity.User;
import com.clinic.repository.NotificationRepository;
import com.clinic.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * EmailNotificationServiceImpl — sends emails and logs every attempt.
 *
 * @Async: email sending is non-blocking so it doesn't delay the HTTP response.
 * On failure: logs error and saves failed Notification record for admin review.
 * ISO/IEC 27001: full audit trail in Notification table.
 *
 * Future extension: add SmsNotificationServiceImpl that also implements
 * NotificationService and inject both via a list / strategy pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationServiceImpl implements NotificationService {

    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    @Async
    @Override
    public void sendAppointmentConfirmation(Appointment appointment) {
        User user = appointment.getPatient().getUser();
        if (!appointment.getPatient().isEmailConsent()) return;

        String subject = "Appointment Confirmed — " + appointment.getAppointmentDate();
        String body = buildConfirmationBody(appointment);
        send(user, subject, body, "CONFIRMATION");
    }

    @Async
    @Override
    public void sendRescheduleNotification(Appointment appointment) {
        User user = appointment.getPatient().getUser();
        if (!appointment.getPatient().isEmailConsent()) return;

        String subject = "Appointment Rescheduled — " + appointment.getAppointmentDate();
        String body = "Your appointment has been rescheduled to " +
                appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() +
                " with Dr. " + appointment.getDoctor().getUser().getFullName();
        send(user, subject, body, "RESCHEDULE");
    }

    @Async
    @Override
    public void sendCancellationNotification(Appointment appointment) {
        User user = appointment.getPatient().getUser();
        if (!appointment.getPatient().isEmailConsent()) return;

        String subject = "Appointment Cancelled";
        String body = "Your appointment on " + appointment.getAppointmentDate() +
                " at " + appointment.getAppointmentTime() +
                " with Dr. " + appointment.getDoctor().getUser().getFullName() +
                " has been cancelled.\nReason: " + appointment.getCancellationReason();
        send(user, subject, body, "CANCELLATION");
    }

    @Async
    @Override
    public void sendAppointmentReminder(Appointment appointment) {
        User user = appointment.getPatient().getUser();
        if (!appointment.getPatient().isEmailConsent()) return;

        String subject = "Reminder: Appointment Tomorrow — " + appointment.getAppointmentDate();
        String body = "This is a reminder for your appointment tomorrow:\n" +
                "Doctor: Dr. " + appointment.getDoctor().getUser().getFullName() + "\n" +
                "Date: " + appointment.getAppointmentDate() + "\n" +
                "Time: " + appointment.getAppointmentTime() + "\n\n" +
                "Please arrive 10 minutes early for check-in.";
        send(user, subject, body, "REMINDER");
    }

    @Async
    @Override
    public void sendQueueUpdateNotification(User user, int queuePosition, int estimatedWaitMinutes) {
        String subject = "Queue Update — Your position: #" + queuePosition;
        String body = "You are now at position #" + queuePosition +
                " in the queue. Estimated wait: " + estimatedWaitMinutes + " minutes.";
        send(user, subject, body, "QUEUE_UPDATE");
    }

    // ─── Private: send + log ──────────────────────────────────────────────────

    private void send(User user, String subject, String body, String type) {
        Notification.NotificationBuilder log = Notification.builder()
                .user(user).channel("EMAIL").type(type)
                .subject(subject).body(body).sentAt(LocalDateTime.now());
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(user.getEmail());
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);

            notificationRepository.save(log.success(true).build());
            this.log.info("Email [{}] sent to {}", type, user.getEmail());

        } catch (MailException e) {
            notificationRepository.save(log.success(false).errorMessage(e.getMessage()).build());
            this.log.error("Failed to send email [{}] to {}: {}", type, user.getEmail(), e.getMessage());
        }
    }

    private String buildConfirmationBody(Appointment a) {
        return String.format(
                "Dear %s,\n\nYour appointment has been confirmed:\n" +
                "Doctor:  Dr. %s (%s)\n" +
                "Date:    %s\n" +
                "Time:    %s\n\n" +
                "Please arrive 10 minutes early for check-in.\n\n" +
                "To cancel or reschedule, log in to the clinic portal.\n\n" +
                "Local Clinic System",
                a.getPatient().getUser().getFullName(),
                a.getDoctor().getUser().getFullName(),
                a.getDoctor().getSpecialization(),
                a.getAppointmentDate(),
                a.getAppointmentTime()
        );
    }
}
