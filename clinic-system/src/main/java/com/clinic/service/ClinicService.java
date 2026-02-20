package com.clinic.service;

import com.clinic.dto.request.ClinicCreateRequest;
import com.clinic.dto.request.ClinicAdminCreateRequest;
import com.clinic.dto.response.ClinicResponse;
import com.clinic.dto.response.PlatformStatsResponse;

import java.util.List;

/**
 * ClinicService — Business Layer for clinic lifecycle management.
 *
 * All methods here are callable ONLY by SUPER_ADMIN.
 * Enforced via @PreAuthorize("hasRole('SUPER_ADMIN')") in the controller.
 *
 * Responsibilities:
 *  - Create and configure clinics
 *  - Assign clinic administrators
 *  - Activate / suspend / deactivate clinics
 *  - Retrieve platform-wide statistics
 *  - Adjust per-clinic limits (maxDoctors, subscription plan)
 */
public interface ClinicService {

    /** Create a new clinic — status starts as PENDING */
    ClinicResponse createClinic(ClinicCreateRequest request);

    /**
     * Create a clinic admin user and associate them with the given clinic.
     * The admin user gets Role.ADMIN and clinic = the target clinic.
     * SUPER_ADMIN can create multiple admins per clinic.
     */
    void assignClinicAdmin(Long clinicId, ClinicAdminCreateRequest request);

    /** Set clinic status to ACTIVE (from PENDING or SUSPENDED) */
    ClinicResponse activateClinic(Long clinicId);

    /**
     * Suspend a clinic — immediately blocks ALL users of that clinic
     * via ClinicContextFilter's status check on every request.
     */
    ClinicResponse suspendClinic(Long clinicId, String reason);

    /** Permanently deactivate (soft-close) a clinic */
    ClinicResponse deactivateClinic(Long clinicId, String reason);

    /** Update clinic configuration (maxDoctors, subscriptionPlan, notes) */
    ClinicResponse updateClinicConfig(Long clinicId, ClinicCreateRequest request);

    ClinicResponse getClinicById(Long clinicId);

    List<ClinicResponse> getAllClinics();

    List<ClinicResponse> getClinicsByStatus(String status);

    /**
     * Platform-wide aggregate statistics for the super admin dashboard:
     *  - total clinics by status
     *  - total doctors / patients / appointments across all clinics
     *  - notification delivery rate platform-wide
     */
    PlatformStatsResponse getPlatformStats();
}
