package com.clinic.service;

import com.clinic.dto.response.DailyReportResponse;

import java.time.LocalDate;

/**
 * AdminReportService — aggregates quality metrics for the admin dashboard.
 *
 * Metrics tracked (from Week 3 quality framework):
 *  - Total appointments per day
 *  - Appointment conflict rate (always 0 by design)
 *  - Average wait time (from QueueEntry timestamps)
 *  - Notification delivery rate (successes / total)
 *  - Walk-in vs appointment ratio
 */
public interface AdminReportService {

    DailyReportResponse getDailyReport(LocalDate date);

    DailyReportResponse getDateRangeReport(LocalDate from, LocalDate to);
}
