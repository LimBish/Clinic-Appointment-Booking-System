package com.clinic.service;

import com.clinic.model.entity.Appointment;
import com.clinic.model.entity.User;

/**
 * NotificationService — Business Layer interface for all notifications.
 *
 * Implementations handle Email (and optionally SMS).
 * Every sent notification is logged in the Notification table
 * for ISO/IEC 27001 audit trail and admin delivery-rate reporting.
 */
public interface NotificationService {

    void sendAppointmentConfirmation(Appointment appointment);

    void sendRescheduleNotification(Appointment appointment);

    void sendCancellationNotification(Appointment appointment);

    void sendAppointmentReminder(Appointment appointment);

    void sendQueueUpdateNotification(User user, int queuePosition, int estimatedWaitMinutes);
}
