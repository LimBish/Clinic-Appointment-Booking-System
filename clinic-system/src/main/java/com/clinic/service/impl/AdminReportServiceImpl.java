package com.clinic.service.impl;

import com.clinic.dto.response.DailyReportResponse;
import com.clinic.model.entity.Appointment;
import com.clinic.model.entity.QueueEntry;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.QueueEntryRepository;
import com.clinic.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportServiceImpl implements AdminReportService {

    private final AppointmentRepository appointmentRepository;
    private final QueueEntryRepository queueEntryRepository;

    @Override
    public DailyReportResponse getDailyReport(LocalDate date) {

        List<Appointment> appointments =
                appointmentRepository.findByAppointmentDate(date);

        List<QueueEntry> queueEntries = queueEntryRepository.findByQueueDate(date);

        return buildReport(date, appointments, queueEntries);
    }


    private DailyReportResponse buildReport(LocalDate date,
                                            List<Appointment> appointments,
                                            List<QueueEntry> queueEntries) {

        long totalAppointments = appointments.size();

        long confirmedAppointments = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .count();

        long cancelledAppointments = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED)
                .count();

        long completedAppointments = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .count();

        long noShowAppointments = appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW)
                .count();

        long totalQueueEntries = queueEntries.size();

        long walkInCount = queueEntries.stream()
                .filter(QueueEntry::isWalkIn)
                .count();

        long appointmentCheckIns = totalQueueEntries - walkInCount;

        // ─── Average Wait Time (checkIn → consultStart) ─────────────
        double avgWaitTime = queueEntries.stream()
                .filter(q -> q.getCheckInTime() != null && q.getConsultStartTime() != null)
                .mapToLong(q ->
                        java.time.Duration
                                .between(q.getCheckInTime(), q.getConsultStartTime())
                                .toMinutes()
                )
                .average()
                .orElse(0);

        // If you are not tracking notifications yet
        double notificationRate = 100.0; // or set to 0 if preferred

        return DailyReportResponse.builder()
                .date(date)
                .totalAppointments(totalAppointments)
                .confirmedAppointments(confirmedAppointments)
                .cancelledAppointments(cancelledAppointments)
                .completedAppointments(completedAppointments)
                .noShowAppointments(noShowAppointments)
                .totalQueueEntries(totalQueueEntries)
                .walkInCount(walkInCount)
                .appointmentCheckIns(appointmentCheckIns)
                .avgWaitTimeMinutes(avgWaitTime)
                .notificationDeliveryRatePercent(notificationRate)
                .appointmentConflicts(0) // always 0 by business rule
                .build();
    }



    @Override
    public DailyReportResponse getDateRangeReport(LocalDate from, LocalDate to) {

        LocalDate current = from;

        long totalAppointments = 0;
        long confirmed = 0;
        long cancelled = 0;
        long completed = 0;
        long noShow = 0;
        long totalQueueEntries = 0;
        long walkIns = 0;
        long appointmentCheckIns = 0;
        double totalWaitTime = 0;
        long waitCount = 0;

        while (!current.isAfter(to)) {

            DailyReportResponse daily = getDailyReport(current);

            totalAppointments += daily.getTotalAppointments();
            confirmed += daily.getConfirmedAppointments();
            cancelled += daily.getCancelledAppointments();
            completed += daily.getCompletedAppointments();
            noShow += daily.getNoShowAppointments();
            totalQueueEntries += daily.getTotalQueueEntries();
            walkIns += daily.getWalkInCount();
            appointmentCheckIns += daily.getAppointmentCheckIns();

            if (daily.getAvgWaitTimeMinutes() > 0) {
                totalWaitTime += daily.getAvgWaitTimeMinutes();
                waitCount++;
            }

            current = current.plusDays(1);
        }

        double avgWait = waitCount == 0 ? 0 : totalWaitTime / waitCount;

        return DailyReportResponse.builder()
                .date(from) // representative start date
                .totalAppointments(totalAppointments)
                .confirmedAppointments(confirmed)
                .cancelledAppointments(cancelled)
                .completedAppointments(completed)
                .noShowAppointments(noShow)
                .totalQueueEntries(totalQueueEntries)
                .walkInCount(walkIns)
                .appointmentCheckIns(appointmentCheckIns)
                .avgWaitTimeMinutes(avgWait)
                .notificationDeliveryRatePercent(0) // adjust if tracked
                .appointmentConflicts(0)
                .build();
    }




//    private DailyReportResponse buildReport(LocalDate date,
//                                            List<Appointment> appointments,
//                                            List<QueueEntry> queueEntries) {
//
//        long totalAppointments = appointments.size();
//
//        long walkIns = appointments.stream()
//                .filter(Appointment::isWalkIn)
//                .count();
//
//        long scheduled = totalAppointments - walkIns;
//
//        double walkInRatio = totalAppointments == 0
//                ? 0
//                : (double) walkIns / totalAppointments;
//
//        double averageWaitTime = queueEntries.stream()
//                .filter(q -> q.getCalledAt() != null)
//                .mapToLong(q ->
//                        Duration.between(q.getCreatedAt(), q.getCalledAt()).toMinutes()
//                )
//                .average()
//                .orElse(0);
//
//        return DailyReportResponse.builder()
//                .date(date)
//                .totalAppointments(totalAppointments)
//                .conflictRate(0.0) // always 0 (business rule)
//                .averageWaitTimeMinutes(averageWaitTime)
//                .walkInRatio(walkInRatio)
//                .scheduledRatio(1 - walkInRatio)
//                .build();
//    }
}
