package com.clinic.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DoctorSchedule — defines a doctor's availability for a specific day of the week.
 *
 * Example: Dr. Smith is available Monday 09:00–13:00 and 14:00–17:00.
 * Two DoctorSchedule rows cover that (one per block).
 *
 * The AppointmentService uses this to generate available time slots
 * and prevent bookings outside defined hours (anti-conflict logic).
 */
@Entity
@Table(name = "doctor_schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    /**
     * Soft-delete / holiday block:
     * Admin can disable a schedule slot without deleting it.
     */
    @Column(nullable = false)
    private boolean active = true;
}
