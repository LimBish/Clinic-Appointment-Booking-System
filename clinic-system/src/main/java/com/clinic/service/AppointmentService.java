package com.clinic.service;

import com.clinic.dto.request.AppointmentBookRequest;
import com.clinic.dto.request.AppointmentRescheduleRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.AvailableSlotResponse;
import com.clinic.model.entity.Appointment;

import java.time.LocalDate;
import java.util.List;

/**
 * AppointmentService — Business Layer interface for appointment operations.
 *
 * All public methods are the "contract" between the Controller and the
 * business logic. Implementations live in service/impl.
 *
 * Core business rules:
 *  BR-001  No double booking: same doctor, same date+time.
 *  BR-002  Appointments only within doctor's defined schedule slots.
 *  BR-003  Max appointments per doctor per day enforced.
 *  BR-004  Only PENDING/CONFIRMED appointments may be rescheduled.
 *  BR-005  Reminders dispatched 24 hours before appointment time.
 */
public interface AppointmentService {

    /**
     * Books a new appointment for the logged-in patient.
     * Validates conflict (BR-001), schedule bounds (BR-002), daily cap (BR-003).
     */
    AppointmentResponse bookAppointment(Long patientUserId, AppointmentBookRequest request);

    /**
     * Reschedules an existing PENDING/CONFIRMED appointment (BR-004).
     * Treats rescheduling as cancel + re-book with conflict checks.
     */
    AppointmentResponse rescheduleAppointment(Long appointmentId, Long patientUserId,
                                               AppointmentRescheduleRequest request);

    /**
     * Cancels an appointment (patient or admin).
     * Sets status to CANCELLED and records the reason.
     */
    void cancelAppointment(Long appointmentId, Long requestingUserId, String reason);

    /** Returns all time slots available for a given doctor on a given date. */
    List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date);

    /** Patient's full appointment history (newest first). */
    List<AppointmentResponse> getPatientAppointments(Long patientUserId);

    /** Upcoming appointments only (today and future). */
    List<AppointmentResponse> getUpcomingAppointments(Long patientUserId);

    /** Doctor's appointment list for a specific date. */
    List<AppointmentResponse> getDoctorDailySchedule(Long doctorId, LocalDate date);

    /** Doctor's appointments for a full week. */
    List<AppointmentResponse> getDoctorWeeklySchedule(Long doctorId, LocalDate weekStart);

    /** Single appointment detail (access-checked by service). */
    AppointmentResponse getAppointmentById(Long appointmentId, Long requestingUserId);

    /** Admin: all appointments within date range for reporting. */
//    List<AppointmentResponse> getAppointmentsByDateRange(LocalDate start, LocalDate end);

    List<AppointmentResponse> getAppointmentsByDateRange(
            Long clinicId,
            LocalDate start,
            LocalDate end);


    /** Admin / Doctor: mark appointment as COMPLETED after consultation. */
    AppointmentResponse completeAppointment(Long appointmentId, Long doctorUserId);

//    List<Appointment> getAppointment(String active);
}
