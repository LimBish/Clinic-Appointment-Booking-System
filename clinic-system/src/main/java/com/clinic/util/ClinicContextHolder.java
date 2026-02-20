package com.clinic.util;

import com.clinic.model.entity.Clinic;

/**
 * ClinicContextHolder — thread-local storage of the current request's Clinic.
 *
 * Works like Spring Security's SecurityContextHolder but for the tenant.
 *
 * Flow:
 *   1. ClinicContextFilter runs on every authenticated request.
 *   2. It loads the logged-in user's clinic from the DB and stores it here.
 *   3. Service methods call ClinicContextHolder.getRequiredClinic() to get
 *      the current tenant — no need to pass Clinic as a parameter everywhere.
 *   4. Filter clears the holder after the request finishes (thread pool safety).
 *
 * SUPER_ADMIN:
 *   Their clinic is null (no tenant affiliation).
 *   Services check ClinicContextHolder.isSuperAdmin() to decide whether
 *   to apply clinic scoping or run platform-wide queries.
 *
 * Why ThreadLocal and not SecurityContext?
 *   The Clinic entity is a JPA-managed object. Storing it in the
 *   SecurityContext would couple Spring Security to JPA. ThreadLocal
 *   keeps the separation clean.
 */
public class ClinicContextHolder {

    private static final ThreadLocal<Clinic> CLINIC_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPER_ADMIN_HOLDER = new ThreadLocal<>();

    public static void setClinic(Clinic clinic) {
        CLINIC_HOLDER.set(clinic);
        SUPER_ADMIN_HOLDER.set(false);
    }

    public static void setSuperAdmin() {
        CLINIC_HOLDER.set(null);
        SUPER_ADMIN_HOLDER.set(true);
    }

    /**
     * Returns the current clinic, or null if SUPER_ADMIN.
     */
    public static Clinic getClinic() {
        return CLINIC_HOLDER.get();
    }

    /**
     * Returns the current clinic. Throws if called for a SUPER_ADMIN
     * context where clinic is not applicable.
     */
    public static Clinic getRequiredClinic() {
        Clinic clinic = CLINIC_HOLDER.get();
        if (clinic == null) {
            throw new IllegalStateException(
                "No clinic context available. This operation requires a clinic-scoped user.");
        }
        return clinic;
    }

    /**
     * Returns true if the current request is from a SUPER_ADMIN user.
     */
    public static boolean isSuperAdmin() {
        Boolean value = SUPER_ADMIN_HOLDER.get();
        return Boolean.TRUE.equals(value);
    }

    /**
     * Must be called at the end of every request (in the filter's finally block)
     * to prevent memory leaks and tenant bleed-over between requests
     * in the thread pool.
     */
    public static void clear() {
        CLINIC_HOLDER.remove();
        SUPER_ADMIN_HOLDER.remove();
    }
}
