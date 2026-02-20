package com.clinic.util;

import com.clinic.dto.response.QueueEntryResponse;
import com.clinic.model.entity.QueueEntry;
import org.springframework.stereotype.Component;

@Component
public class QueueMapper {

    public QueueEntryResponse toResponse(QueueEntry q) {
        return QueueEntryResponse.builder()
                .id(q.getId())
                .patientId(q.getPatient().getId())
                .patientName(q.getPatient().getUser().getFullName())
                .patientPhone(q.getPatient().getUser().getPhone())
                .doctorId(q.getDoctor().getId())
                .doctorName(q.getDoctor().getUser().getFullName())
                .queueDate(q.getQueueDate())
                .queueNumber(q.getQueueNumber())
                .status(q.getStatus().name())
                .checkInTime(q.getCheckInTime())
                .walkIn(q.isWalkIn())
                .build();
    }
}
