package com.clinic.util;

import com.clinic.dto.response.AppointmentResponse;
import com.clinic.model.entity.Appointment;
import org.springframework.stereotype.Component;

/**
 * AppointmentMapper — converts Appointment entity → AppointmentResponse DTO.
 *
 * Keeps entity objects out of the presentation layer (Thymeleaf templates
 * only ever see DTOs, not JPA-managed entity objects).
 * This is the Mapper pattern — a lightweight alternative to MapStruct for
 * readability in a teaching/academic context.
 */
@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .patientId(a.getPatient().getId())
                .patientName(a.getPatient().getUser().getFullName())
                .doctorId(a.getDoctor().getId())
                .doctorName(a.getDoctor().getUser().getFullName())
                .doctorSpecialization(a.getDoctor().getSpecialization())
                .appointmentDate(a.getAppointmentDate())
                .appointmentTime(a.getAppointmentTime())
                .status(a.getStatus().name())
                .reason(a.getReason())
                .reminderSent(a.isReminderSent())
                .cancellationReason(a.getCancellationReason())
                .build();
    }
}
