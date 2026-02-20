package com.clinic.dto.response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

// ─── QueueEntryResponse ───────────────────────────────────────────────────────
// Passed to patient and admin queue views
// ─────────────────────────────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
class QueueEntryResponseBase {
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

    // Computed fields — set by QueueServiceImpl
    private int patientsAhead;
    private int estimatedWaitMinutes;
}
