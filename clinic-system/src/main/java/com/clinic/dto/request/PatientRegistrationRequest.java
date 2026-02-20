package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * PatientRegistrationRequest — form data for new patient self-registration.
 *
 * Ethics/Autonomy (Week 5): smsConsent and emailConsent are boolean checkboxes.
 * Both default to false (opt-in, not opt-out) so consent is active, not assumed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientRegistrationRequest {

    @NotNull(message = "Please select a clinic")
    private Long clinicId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Invalid phone number")
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    @Size(max = 300)
    private String address;

    /** Active opt-in for email reminders (Ethics: Autonomy) */
    private boolean emailConsent = true;

    /** Active opt-in for SMS reminders (Ethics: Autonomy) */
    private boolean smsConsent = false;
}