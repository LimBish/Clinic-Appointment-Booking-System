package com.clinic.model.enums;

/**
 * System roles for Role-Based Access Control (RBAC).
 * Maps directly to Spring Security GrantedAuthority.
 *
 * SUPER_ADMIN — platform owner; manages ALL clinics, creates clinic accounts,
 *               monitors cross-clinic metrics, suspends/activates clinics.
 *               Has NO clinic affiliation — operates at the platform level.
 *
 * ADMIN       — clinic-level admin; full access within their own clinic only.
 *               Cannot see data from other clinics.
 *
 * DOCTOR      — can manage own schedule, view appointments, mark as completed.
 *               Scoped to their clinic.
 *
 * PATIENT     — can book, reschedule, cancel appointments; view queue position.
 *               Registered under a specific clinic.
 */
public enum Role {
    SUPER_ADMIN,
    ADMIN,
    DOCTOR,
    PATIENT
}
