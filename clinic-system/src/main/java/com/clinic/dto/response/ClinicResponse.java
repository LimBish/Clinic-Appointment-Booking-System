package com.clinic.dto.response;

import lombok.*;
import java.time.LocalDateTime;

/**
 * ClinicResponse — data returned to SUPER_ADMIN views for a clinic.
 * Includes live counts (doctors, patients) computed at query time.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicResponse {
    private Long id;
    private String name;
    private String registrationCode;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String status;
    private int maxDoctors;
    private String subscriptionPlan;
    private String notes;

    // Computed counts
    private int totalDoctors;
    private int totalPatients;

    private LocalDateTime createdAt;
}
