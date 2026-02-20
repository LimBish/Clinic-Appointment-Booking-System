package com.clinic.model.entity;

import com.clinic.model.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Appointment — the core booking record.
 *
 * Business rules enforced at the service layer:
 *  1. No two CONFIRMED appointments for the same doctor on the same date+time.
 *  2. appointmentDate must fall within the doctor's DoctorSchedule.
 *  3. Total confirmed appointments per doctor per day ≤ maxDailyAppointments.
 *  4. Only PENDING or CONFIRMED appointments can be rescheduled or cancelled.
 *
 * Reminder flow:
 *  reminderSent = false initially.
 *  The AppointmentReminderScheduler flips it to true after sending email/SMS.
 */
@Entity
@Table(name = "appointments",
        indexes = {
            @Index(name = "idx_appointment_clinic_date",  columnList = "clinic_id, appointment_date"),
            @Index(name = "idx_appointment_doctor_date",  columnList = "doctor_id, appointment_date"),
            @Index(name = "idx_appointment_patient",      columnList = "patient_id")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Denormalized tenant FK for high-performance cross-cutting queries.
     * SUPER_ADMIN can query all appointments across all clinics using this column.
     * Could be derived via patient.clinic, but having it directly avoids joins
     * in the hot path (especially scheduler and reporting queries).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;

    /** Free-text reason for visit — kept vague, no clinical notes */
    @Column(length = 500)
    private String reason;

    /**
     * Tracks whether the 24-hour reminder has been dispatched.
     * Prevents duplicate reminders on scheduler re-runs.
     */
    @Column(nullable = false)
    private boolean reminderSent = false;

    /** Optional cancellation reason captured for admin reporting */
    @Column(length = 300)
    private String cancellationReason;

    // ─── Relationships ───────────────────────────────────────────────────────

    /** Created when patient checks in at the clinic */
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private QueueEntry queueEntry;
}
