package com.clinic.model.entity;

import com.clinic.model.enums.ClinicStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Clinic — the root tenant entity in the multi-clinic architecture.
 *
 * Every piece of operational data (Doctors, Patients, Appointments, QueueEntries)
 * is associated with exactly one Clinic. This provides logical data isolation:
 * Clinic A's admin NEVER sees Clinic B's data.
 *
 * Multi-tenancy strategy: LOGICAL isolation via clinic_id FK on all tenant-scoped
 * entities + enforcement in the service layer. A single application instance
 * and single database serve all clinics, separated by clinic_id.
 * (ISO/IEC 27001 Week 5: "strict clinic-level data isolation in multi-tenant deployment")
 *
 * Only SUPER_ADMIN can create, activate, suspend, or delete clinics.
 *
 * Fields:
 *  name            — display name, e.g. "Kathmandu Family Clinic"
 *  registrationCode — unique short code (e.g. "KFC-001"), used in reports
 *  address / city  — location details
 *  phone / email   — clinic contact (not a user account)
 *  status          — PENDING → ACTIVE → SUSPENDED / INACTIVE
 *  maxDoctors      — platform-enforced cap set by super admin (per plan/subscription)
 *  subscriptionPlan — BASIC / STANDARD / PREMIUM (for future billing)
 *  notes           — super admin internal notes about this clinic
 */
@Entity
@Table(name = "clinics",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "registrationCode"),
            @UniqueConstraint(columnNames = "email")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Clinic extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Unique short code assigned by SUPER_ADMIN.
     * Used in reports and logs to identify the clinic without exposing full name.
     * Example: "KFC-001", "PKR-002"
     */
    @Column(nullable = false, unique = true, length = 20)
    private String registrationCode;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 20)
    private String phone;

    /** Clinic contact email — NOT the admin login email */
    @Column(unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClinicStatus status = ClinicStatus.PENDING;

    /**
     * Platform-level cap on doctors per clinic.
     * Enforced at ClinicService when ADMIN tries to add a new doctor.
     * SUPER_ADMIN can adjust this per clinic (like a subscription plan).
     */
    @Column(nullable = false)
    private int maxDoctors = 10;

    @Column(length = 50)
    private String subscriptionPlan = "STANDARD";

    /** Internal notes visible only to SUPER_ADMIN */
    @Column(length = 1000)
    private String notes;

    // ─── Relationships ────────────────────────────────────────────────────────

    /**
     * All users belonging to this clinic (ADMINs, DOCTORs, PATIENTs).
     * SUPER_ADMIN users have clinic = null (platform-level).
     */
    @OneToMany(mappedBy = "clinic", fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Doctor> doctors = new ArrayList<>();

    @OneToMany(mappedBy = "clinic", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Patient> patients = new ArrayList<>();
}
