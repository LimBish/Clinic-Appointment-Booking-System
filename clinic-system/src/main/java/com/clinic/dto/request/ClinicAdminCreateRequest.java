package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ClinicAdminCreateRequest — SUPER_ADMIN creates a clinic admin account.
 * The new user is auto-assigned Role.ADMIN and linked to the target clinic.
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicAdminCreateRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$")
    private String phone;
}
