package com.clinic.model.entity;

import com.clinic.model.enums.QueueStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * QueueEntry — represents a patient's position in the live daily queue.
 *
 * Created when:
 *  a) A patient checks in against a CONFIRMED appointment, or
 *  b) Admin registers a walk-in patient directly.
 *
 * Queue position logic:
 *  queueNumber is auto-assigned as (max queueNumber for doctor+date) + 1.
 *  The QueueService exposes this number to the patient in real-time via
 *  the Thymeleaf page (auto-refreshed every 30 seconds via meta refresh).
 *
 * Estimated wait:
 *  estimatedWaitMinutes = (queueNumber - currentlyServing) * avgMinutesPerPatient
 */
@Entity
@Table(name = "queue_entries",
        indexes = {
            @Index(name = "idx_queue_clinic_date",  columnList = "clinic_id, queue_date"),
            @Index(name = "idx_queue_doctor_date",  columnList = "doctor_id, queue_date")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QueueEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Denormalized tenant FK — same rationale as on Appointment */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    /** Nullable — walk-in patients have no prior appointment */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false)
    private LocalDate queueDate;

    /** Sequential number within doctor's queue for the day */
    @Column(nullable = false)
    private int queueNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.WAITING;

    /** Timestamp when patient physically checked in */
    @Column
    private LocalDateTime checkInTime;

    /** Timestamp when consultation started */
    @Column
    private LocalDateTime consultStartTime;

    /** Timestamp when consultation ended */
    @Column
    private LocalDateTime consultEndTime;

    /** Calculated on-the-fly; optionally persisted for reporting */
    @Column
    private Integer estimatedWaitMinutes;

    /** True = walk-in patient, no prior appointment */
    @Column(nullable = false)
    private boolean walkIn = false;
}
