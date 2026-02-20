package com.clinic.service;

import com.clinic.dto.response.QueueEntryResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * QueueService — Business Layer interface for real-time queue management.
 *
 * Queue rules:
 *  QR-001  A patient can be in only one active queue per day per doctor.
 *  QR-002  Queue number is assigned sequentially; never reused.
 *  QR-003  Estimated wait = patientsAhead × avgMinutesPerPatient.
 *  QR-004  Walk-in patients join the queue without a prior appointment.
 */
public interface QueueService {

    /** Patient checks in against a confirmed appointment — creates QueueEntry. */
    QueueEntryResponse checkIn(Long appointmentId, Long patientUserId);

    /** Admin registers a walk-in patient into a doctor's queue. */
    QueueEntryResponse addWalkIn(Long doctorId, Long patientUserId);

    /** Patient's current queue position and estimated wait. */
    QueueEntryResponse getMyQueueStatus(Long patientUserId, LocalDate date);

    /** Full live queue for a doctor today — admin/doctor view. */
    List<QueueEntryResponse> getDoctorQueue(Long doctorId, LocalDate date);

    /** Admin: advance to next patient (set current IN_CONSULT, mark previous DONE). */
    QueueEntryResponse callNextPatient(Long doctorId);

    /** Admin: skip a patient (e.g., stepped out). */
    QueueEntryResponse skipPatient(Long queueEntryId);

    /** Mark consultation complete — set status DONE, record end time. */
    QueueEntryResponse completeConsultation(Long queueEntryId);
}
