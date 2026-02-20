package com.clinic.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRescheduleRequest {

    @NotNull(message = "Please select a new date")
    @Future(message = "New appointment date must be in the future")
    private LocalDate newDate;

    @NotNull(message = "Please select a new time slot")
    private LocalTime newTime;
}
