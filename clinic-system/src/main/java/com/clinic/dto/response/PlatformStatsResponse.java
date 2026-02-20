package com.clinic.dto.response;

import lombok.*;

/**
 * PlatformStatsResponse — aggregate statistics across ALL clinics.
 * Displayed on the SUPER_ADMIN dashboard.
 *
 * Maps to the Week 3 quality metrics framework but at the platform level.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlatformStatsResponse {

    // ─── Clinic counts ────────────────────────────────────────────────────────
    private long totalClinics;
    private long activeClinics;
    private long pendingClinics;
    private long suspendedClinics;

    // ─── People counts ────────────────────────────────────────────────────────
    private long totalDoctors;
    private long totalPatients;

    // ─── Activity ─────────────────────────────────────────────────────────────
    private long totalAppointments;

    // ─── Quality metrics ──────────────────────────────────────────────────────
    private double notificationDeliveryRatePercent;
}
