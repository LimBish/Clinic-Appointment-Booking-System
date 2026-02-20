package com.clinic.repository;

import com.clinic.model.entity.Doctor;
import com.clinic.model.entity.Patient;
import com.clinic.model.entity.QueueEntry;
import com.clinic.model.enums.QueueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    List<QueueEntry> findByDoctorAndQueueDateOrderByQueueNumberAsc(Doctor doctor, LocalDate date);

    List<QueueEntry> findByDoctorAndQueueDateAndStatusOrderByQueueNumberAsc(
            Doctor doctor, LocalDate date, QueueStatus status);
//    List<QueueEntry> findByDate(LocalDate date);

    List<QueueEntry> findByQueueDate(LocalDate queueDate);

//    List<QueueEntry> findByDateBetween(LocalDate from, LocalDate to);

    List<QueueEntry> findByQueueDateBetween(LocalDate start, LocalDate end);

    Optional<QueueEntry> findByPatientAndQueueDateAndStatusIn(
            Patient patient, LocalDate date, List<QueueStatus> statuses);

    /** Next queue number = current max + 1 for the doctor's queue today */
    @Query("""
            SELECT COALESCE(MAX(q.queueNumber), 0) + 1
            FROM QueueEntry q
            WHERE q.doctor = :doctor AND q.queueDate = :date
            """)
    int nextQueueNumber(@Param("doctor") Doctor doctor, @Param("date") LocalDate date);

    /** Count of patients still WAITING ahead of a given queue number */
    @Query("""
            SELECT COUNT(q) FROM QueueEntry q
            WHERE q.doctor = :doctor
              AND q.queueDate = :date
              AND q.status = 'WAITING'
              AND q.queueNumber < :queueNumber
            """)
    long countAhead(
            @Param("doctor") Doctor doctor,
            @Param("date") LocalDate date,
            @Param("queueNumber") int queueNumber
    );

    /** Daily count for admin reporting */
    long countByQueueDate(LocalDate date);

    long countByDoctorAndQueueDateAndStatus(Doctor doctor, LocalDate date, QueueStatus status);
}
