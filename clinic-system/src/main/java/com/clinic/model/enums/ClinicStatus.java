package com.clinic.model.enums;

/**
 * ClinicStatus — lifecycle states of a clinic registration.
 *
 * ACTIVE    — fully operational; all features available.
 * SUSPENDED — temporarily deactivated by SUPER_ADMIN (e.g., billing issue).
 *             Logins are blocked for all users of this clinic.
 * INACTIVE  — closed or decommissioned; data retained for audit purposes.
 * PENDING   — newly registered, awaiting SUPER_ADMIN approval/activation.
 */
public enum ClinicStatus {
    PENDING,
    ACTIVE,
    SUSPENDED,
    INACTIVE
}
