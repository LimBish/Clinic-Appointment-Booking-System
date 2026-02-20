package com.clinic.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * AppointmentResponse — data returned to the view layer for an appointment.
 * Never exposes raw entity objects to templates (layer isolation).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String status;
    private String reason;
    private boolean reminderSent;
    private String cancellationReason;
}
