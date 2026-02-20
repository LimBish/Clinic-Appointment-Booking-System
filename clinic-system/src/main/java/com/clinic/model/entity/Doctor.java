package com.clinic.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Doctor — profile data for users with role DOCTOR.
 *
 * A Doctor has:
 *   - specialization: e.g. General Physician, Cardiologist
 *   - availabilityNotes: free-text block-out notes (e.g. "Off Fridays")
 *   - maxDailyAppointments: clinic-configurable cap per doctor per day
 *
 * Linked to DoctorSchedule for structured weekly availability.
 */
@Entity
@Table(name = "doctors")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Doctor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Tenant FK — the clinic this doctor belongs to.
     * All appointment queries for this doctor are implicitly scoped to this clinic.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column(nullable = false)
    private String specialization;

    @Column(length = 200)
    private String qualification;

    @Column(length = 500)
    private String bio;

    /** Default room or consultation area */
    @Column(length = 50)
    private String consultationRoom;

    /**
     * Hard cap enforced by the business layer to prevent overbooking.
     * Defaults to the clinic-wide setting but can be customised per doctor.
     */
    @Column(nullable = false)
    private int maxDailyAppointments = 20;

    @Column(nullable = false)
    private boolean available = true;

    // ─── Relationships ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DoctorSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();
}
