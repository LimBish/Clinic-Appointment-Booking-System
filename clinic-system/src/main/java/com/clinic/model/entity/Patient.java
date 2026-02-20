package com.clinic.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Patient — profile data for users with role PATIENT.
 *
 * Ethics / Privacy (Week 5 requirement):
 *   - Only scheduling-relevant data is stored (no detailed medical records).
 *   - dateOfBirth stored for age-group analytics only.
 *   - smsConsent flag — patient explicitly opts in for SMS reminders (Autonomy principle).
 *   - emailConsent flag — GDPR-aligned explicit consent for email reminders.
 */
@Entity
@Table(name = "patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Patient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Tenant FK — the clinic where this patient is registered.
     * A patient registers under ONE clinic. If they visit another clinic
     * they would create a new account (or a future cross-clinic feature
     * would handle patient transfers via SUPER_ADMIN).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private Clinic clinic;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 10)
    private String gender;

    @Column(length = 500)
    private String address;

    /**
     * Ethics/Autonomy: explicit opt-in for SMS reminders.
     * Defaults to false — must be actively enabled.
     */
    @Column(nullable = false)
    private boolean smsConsent = false;

    /**
     * Ethics/Autonomy: explicit opt-in for email reminders.
     */
    @Column(nullable = false)
    private boolean emailConsent = true;

    // ─── Relationships ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Appointment> appointments = new ArrayList<>();

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QueueEntry> queueEntries = new ArrayList<>();
}
