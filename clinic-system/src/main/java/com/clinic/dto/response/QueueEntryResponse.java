package com.clinic.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QueueEntryResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private Long doctorId;
    private String doctorName;
    private LocalDate queueDate;
    private int queueNumber;
    private String status;
    private LocalDateTime checkInTime;
    private boolean walkIn;
    private int patientsAhead;
    private int estimatedWaitMinutes;
}
