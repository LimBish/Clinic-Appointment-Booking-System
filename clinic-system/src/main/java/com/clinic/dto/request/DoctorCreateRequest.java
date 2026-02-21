package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

/**
 * ClinicCreateRequest — form data for SUPER_ADMIN to create or update a clinic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorCreateRequest {

    @NotNull(message = "Please select a clinic")
    private Long clinicId;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s]{7,15}$", message = "Invalid phone number")
    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String qualification;

    private String bio;
    private String specialization;
    private String consultationRoom;
    private int maxDailyAppointments;

    @Size(max = 300)
    private String address;

    /** Active opt-in for email reminders (Ethics: Autonomy) */
    private boolean emailConsent = true;

    /** Active opt-in for SMS reminders (Ethics: Autonomy) */
    private boolean smsConsent = false;
}
