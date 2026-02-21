package com.clinic.model.entity;

import com.clinic.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;

/**
 * User — the central authentication entity.
 *
 * Multi-clinic change:
 *   The 'clinic' field is the tenant discriminator.
 *   - SUPER_ADMIN: clinic = null  (platform-wide, no tenant affiliation)
 *   - ADMIN:       clinic = their assigned clinic
 *   - DOCTOR:      clinic = their assigned clinic
 *   - PATIENT:     clinic = the clinic they registered under
 *
 * The clinic field drives ALL data isolation in the service layer.
 * Every service method queries are scoped to the current user's clinic.
 *
 * ISO/IEC 27001: enabled flag supports access suspension without deletion.
 * When a Clinic is SUSPENDED, the ClinicStatusCheckFilter blocks all
 * clinic-affiliated users from accessing the system.
 */
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-hashed — NEVER stored in plain text */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** ISO/IEC 27001: disable access without deleting audit trail */
    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Tenant FK — null only for SUPER_ADMIN.
     * EAGER fetch allows ClinicStatusCheckFilter to read clinic.status
     * in a single DB round-trip per request.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "clinic_id", nullable = true)
    private Clinic clinic;

    // ─── Relationships ───────────────────────────────────────────────────────

    /** Populated only when role == PATIENT */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Patient patient;

    /** Populated only when role == DOCTOR */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Doctor doctor;
}
