package com.clinic.repository;

import com.clinic.model.entity.Appointment;
import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.Doctor;
import com.clinic.model.entity.Patient;
import com.clinic.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AppointmentRepository — Persistence Layer for Appointment entity.
 *
 * Multi-clinic: all operational queries carry a Clinic parameter.
 * SUPER_ADMIN aggregate queries use clinic-free variants for platform stats.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // ─── Conflict Detection ──────────────────────────────────────────────────

    @Query("""
            SELECT COUNT(a) > 0
            FROM Appointment a
            WHERE a.doctor = :doctor
              AND a.appointmentDate = :date
              AND a.appointmentTime = :time
              AND a.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')
              AND (:excludeId IS NULL OR a.id <> :excludeId)
            """)
    boolean existsConflict(
            @Param("doctor") Doctor doctor,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            @Param("excludeId") Long excludeId
    );

    // ─── Doctor daily cap ─────────────────────────────────────────────────────

    long countByDoctorAndAppointmentDateAndStatusIn(
            Doctor doctor, LocalDate date, List<AppointmentStatus> statuses);

    // ─── Clinic-scoped queries (ADMIN) ────────────────────────────────────────

    List<Appointment> findByClinicAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            Clinic clinic, LocalDate start, LocalDate end);

    long countByClinicAndAppointmentDate(Clinic clinic, LocalDate date);

    long countByClinicAndStatus(Clinic clinic, AppointmentStatus status);

    // ─── Patient queries ──────────────────────────────────────────────────────

    List<Appointment> findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(Patient patient);

    List<Appointment> findByPatientAndStatus(Patient patient, AppointmentStatus status);

    List<Appointment> findByPatientAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
            Patient patient, LocalDate fromDate);

    // ─── Doctor queries ───────────────────────────────────────────────────────

    List<Appointment> findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(
            Doctor doctor, LocalDate date);

    List<Appointment> findByDoctorAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
            Doctor doctor, LocalDate start, LocalDate end);

    List<Appointment> findByAppointmentDate(LocalDate date);

    List<Appointment> findByAppointmentDateBetween(LocalDate from, LocalDate to);


    // ─── Scheduler queries ────────────────────────────────────────────────────

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.status = 'CONFIRMED'
              AND a.reminderSent = false
              AND CAST(CONCAT(CAST(a.appointmentDate AS string), 'T', CAST(a.appointmentTime AS string)) AS java.time.LocalDateTime)
                  BETWEEN :windowStart AND :windowEnd
            """)
    List<Appointment> findReminderCandidates(
            @Param("windowStart") LocalDateTime windowStart,
            @Param("windowEnd") LocalDateTime windowEnd
    );

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.status = 'CONFIRMED'
              AND a.appointmentDate < :today
            """)
    List<Appointment> findOverdueConfirmed(@Param("today") LocalDate today);

    // ─── Platform-level (SUPER_ADMIN) ─────────────────────────────────────────

    /** Total appointments across ALL clinics on a given date */
    long countByAppointmentDate(LocalDate date);

    /** Total appointments per clinic for a date range — used in super admin reports */
    @Query("""
            SELECT a.clinic.id, COUNT(a)
            FROM Appointment a
            WHERE a.appointmentDate BETWEEN :from AND :to
            GROUP BY a.clinic.id
            """)
    List<Object[]> countPerClinicBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Cross-clinic total appointment count */
    long countByStatus(AppointmentStatus status);
}

