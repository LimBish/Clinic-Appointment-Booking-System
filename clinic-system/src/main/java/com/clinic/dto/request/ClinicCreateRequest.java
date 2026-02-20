package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ClinicCreateRequest — form data for SUPER_ADMIN to create or update a clinic.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicCreateRequest {

    @NotBlank(message = "Clinic name is required")
    @Size(min = 3, max = 150)
    private String name;

    /**
     * Short unique code assigned by SUPER_ADMIN.
     * Convention: CITY_ABBREVIATION-NNN (e.g. KTM-001, PKR-002)
     */
    @NotBlank(message = "Registration code is required")
    @Pattern(regexp = "^[A-Z0-9\\-]{3,20}$",
             message = "Code must be 3–20 uppercase alphanumeric characters (hyphens allowed)")
    private String registrationCode;

    @NotBlank(message = "Address is required")
    @Size(max = 500)
    private String address;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "Invalid phone number")
    private String phone;

    @Email(message = "Invalid contact email")
    private String contactEmail;

    /** Maximum number of doctors this clinic is allowed to register */
    @Min(value = 1, message = "Max doctors must be at least 1")
    @Max(value = 200, message = "Max doctors cannot exceed 200")
    private int maxDoctors = 10;

    /** BASIC | STANDARD | PREMIUM */
    private String subscriptionPlan = "STANDARD";

    @Size(max = 1000)
    private String notes;
}
