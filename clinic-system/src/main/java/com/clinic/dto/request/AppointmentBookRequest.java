package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * AppointmentBookRequest — form data for booking a new appointment.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentBookRequest {

    @NotNull(message = "Please select a doctor")
    private Long doctorId;

    @NotNull(message = "Please select a date")
    @Future(message = "Appointment date must be in the future")
    private LocalDate appointmentDate;

    @NotNull(message = "Please select a time slot")
    private LocalTime appointmentTime;

    @Size(max = 500, message = "Reason must be under 500 characters")
    private String reason;
}
