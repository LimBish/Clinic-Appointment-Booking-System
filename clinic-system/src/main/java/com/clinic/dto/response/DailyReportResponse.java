package com.clinic.dto.response;

import lombok.*;
import java.time.LocalDate;

/**
 * DailyReportResponse — aggregated quality metrics for admin reports.
 *
 * Covers all quality metrics defined in Week 3:
 *  - Total appointments booked
 *  - Conflict count (should always be 0)
 *  - Average wait time (minutes)
 *  - Notification delivery rate (%)
 *  - Walk-in vs appointment ratio
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DailyReportResponse {
    private LocalDate date;
    private long totalAppointments;
    private long confirmedAppointments;
    private long cancelledAppointments;
    private long completedAppointments;
    private long noShowAppointments;
    private long totalQueueEntries;
    private long walkInCount;
    private long appointmentCheckIns;
    private double avgWaitTimeMinutes;
    private double notificationDeliveryRatePercent;
    private long appointmentConflicts; // should always be 0
}
