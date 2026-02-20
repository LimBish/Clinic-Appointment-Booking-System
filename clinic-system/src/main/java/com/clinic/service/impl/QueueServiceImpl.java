package com.clinic.service.impl;

import com.clinic.dto.response.QueueEntryResponse;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.model.entity.*;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.QueueStatus;
import com.clinic.repository.*;
import com.clinic.service.QueueService;
import com.clinic.util.QueueMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QueueServiceImpl implements QueueService {

    private final QueueEntryRepository queueEntryRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final QueueMapper queueMapper;

    @Value("${clinic.queue.avg-minutes-per-patient:15}")
    private int avgMinutesPerPatient;

    @Override
    public QueueEntryResponse checkIn(Long appointmentId, Long patientUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED appointments can check in.");
        }

        // QR-001: check not already in queue
        Patient patient = appointment.getPatient();
        queueEntryRepository.findByPatientAndQueueDateAndStatusIn(
                patient, LocalDate.now(), List.of(QueueStatus.WAITING, QueueStatus.IN_CONSULT))
                .ifPresent(q -> {
                    throw new IllegalStateException("Patient is already in the queue (#" + q.getQueueNumber() + ").");
                });

        Doctor doctor = appointment.getDoctor();
        int queueNum = queueEntryRepository.nextQueueNumber(doctor, LocalDate.now());

        QueueEntry entry = QueueEntry.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .queueDate(LocalDate.now())
                .queueNumber(queueNum)
                .status(QueueStatus.WAITING)
                .checkInTime(LocalDateTime.now())
                .walkIn(false)
                .build();

        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        appointmentRepository.save(appointment);

        QueueEntry saved = queueEntryRepository.save(entry);
        return enrichWithWaitTime(queueMapper.toResponse(saved), doctor);
    }

    @Override
    public QueueEntryResponse addWalkIn(Long doctorId, Long patientUserId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient for user", patientUserId));

        int queueNum = queueEntryRepository.nextQueueNumber(doctor, LocalDate.now());

        QueueEntry entry = QueueEntry.builder()
                .patient(patient)
                .doctor(doctor)
                .queueDate(LocalDate.now())
                .queueNumber(queueNum)
                .status(QueueStatus.WAITING)
                .checkInTime(LocalDateTime.now())
                .walkIn(true)
                .build();

        QueueEntry saved = queueEntryRepository.save(entry);
        log.info("Walk-in patient {} added to queue #{} for doctor {}", patientUserId, queueNum, doctorId);
        return enrichWithWaitTime(queueMapper.toResponse(saved), doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public QueueEntryResponse getMyQueueStatus(Long patientUserId, LocalDate date) {
        Patient patient = patientRepository.findByUserId(patientUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient for user", patientUserId));

        QueueEntry entry = queueEntryRepository
                .findByPatientAndQueueDateAndStatusIn(patient, date,
                        List.of(QueueStatus.WAITING, QueueStatus.IN_CONSULT))
                .orElseThrow(() -> new ResourceNotFoundException("Active queue entry for user", patientUserId));

        return enrichWithWaitTime(queueMapper.toResponse(entry), entry.getDoctor());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getDoctorQueue(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
        return queueEntryRepository.findByDoctorAndQueueDateOrderByQueueNumberAsc(doctor, date)
                .stream()
                .map(e -> enrichWithWaitTime(queueMapper.toResponse(e), doctor))
                .collect(Collectors.toList());
    }

    @Override
    public QueueEntryResponse callNextPatient(Long doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        // Mark current IN_CONSULT as DONE
        queueEntryRepository.findByDoctorAndQueueDateAndStatusOrderByQueueNumberAsc(
                        doctor, LocalDate.now(), QueueStatus.IN_CONSULT)
                .stream().findFirst().ifPresent(current -> {
                    current.setStatus(QueueStatus.DONE);
                    current.setConsultEndTime(LocalDateTime.now());
                    queueEntryRepository.save(current);
                });

        // Pull next WAITING patient
        QueueEntry next = queueEntryRepository
                .findByDoctorAndQueueDateAndStatusOrderByQueueNumberAsc(
                        doctor, LocalDate.now(), QueueStatus.WAITING)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No waiting patients", doctorId));

        next.setStatus(QueueStatus.IN_CONSULT);
        next.setConsultStartTime(LocalDateTime.now());
        QueueEntry saved = queueEntryRepository.save(next);
        return queueMapper.toResponse(saved);
    }

    @Override
    public QueueEntryResponse skipPatient(Long queueEntryId) {
        QueueEntry entry = getEntry(queueEntryId);
        entry.setStatus(QueueStatus.SKIPPED);
        return queueMapper.toResponse(queueEntryRepository.save(entry));
    }

    @Override
    public QueueEntryResponse completeConsultation(Long queueEntryId) {
        QueueEntry entry = getEntry(queueEntryId);
        entry.setStatus(QueueStatus.DONE);
        entry.setConsultEndTime(LocalDateTime.now());
        return queueMapper.toResponse(queueEntryRepository.save(entry));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private QueueEntryResponse enrichWithWaitTime(QueueEntryResponse response, Doctor doctor) {
        long ahead = queueEntryRepository.countAhead(
                doctor, LocalDate.now(), response.getQueueNumber());
        response.setPatientsAhead((int) ahead);
        response.setEstimatedWaitMinutes((int) ahead * avgMinutesPerPatient);
        return response;
    }

    private QueueEntry getEntry(Long id) {
        return queueEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QueueEntry", id));
    }
}
