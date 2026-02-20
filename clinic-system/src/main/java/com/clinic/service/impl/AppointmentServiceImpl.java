package com.clinic.service.impl;

import com.clinic.dto.request.AppointmentBookRequest;
import com.clinic.dto.request.AppointmentRescheduleRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.AvailableSlotResponse;
import com.clinic.exception.AppointmentConflictException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.exception.UnauthorizedAccessException;
import com.clinic.model.entity.*;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.repository.*;
import com.clinic.service.AppointmentService;
import com.clinic.service.NotificationService;
import com.clinic.util.AppointmentMapper;
import com.clinic.util.ClinicContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AppointmentServiceImpl — Business Layer implementation.
 *
 * @Transactional ensures all DB operations within a method succeed or
 *                roll back together, maintaining data consistency (no
 *                half-booked slots).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AppointmentMapper appointmentMapper;
    private final ClinicRepository clinicRepository;

    @Value("${clinic.appointment.slot-duration-minutes:30}")
    private int slotDurationMinutes;

    // ─── Book Appointment ─────────────────────────────────────────────────────

    @Override
    public AppointmentResponse bookAppointment(Long patientUserId, AppointmentBookRequest req) {

        Patient patient = getPatientByUserId(patientUserId);
        Doctor doctor = getDoctorById(req.getDoctorId());

        validateSlotAvailability(doctor, req.getAppointmentDate(), req.getAppointmentTime(), null);
        validateDailyCap(doctor, req.getAppointmentDate());

        Appointment appointment = Appointment.builder()
                .clinic(ClinicContextHolder.getRequiredClinic())
                .patient(patient)
                .doctor(doctor)
                .appointmentDate(req.getAppointmentDate())
                .appointmentTime(req.getAppointmentTime())
                .reason(req.getReason())
                .status(AppointmentStatus.CONFIRMED)
                .reminderSent(false)
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment {} booked: Patient {} with Doctor {} on {}",
                saved.getId(), patientUserId, req.getDoctorId(), req.getAppointmentDate());

        notificationService.sendAppointmentConfirmation(saved);
        return appointmentMapper.toResponse(saved);
    }

    // ─── Reschedule Appointment ───────────────────────────────────────────────

    @Override
    public AppointmentResponse rescheduleAppointment(Long appointmentId, Long patientUserId,
                                                     AppointmentRescheduleRequest req) {
        Appointment appointment = getAppointmentAndVerifyOwner(appointmentId, patientUserId);

        if (!List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
                .contains(appointment.getStatus())) {
            throw new IllegalStateException(
                    "Only PENDING or CONFIRMED appointments can be rescheduled.");
        }

        Doctor doctor = appointment.getDoctor();
        validateSlotAvailability(doctor, req.getNewDate(), req.getNewTime(), appointmentId);
        validateDailyCap(doctor, req.getNewDate());

        appointment.setAppointmentDate(req.getNewDate());
        appointment.setAppointmentTime(req.getNewTime());
        appointment.setReminderSent(false); // reset so reminder fires for new time

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment {} rescheduled to {}", appointmentId, req.getNewDate());

        notificationService.sendRescheduleNotification(saved);
        return appointmentMapper.toResponse(saved);
    }

    // ─── Cancel Appointment ───────────────────────────────────────────────────

    @Override
    public void cancelAppointment(Long appointmentId, Long requestingUserId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason);
        appointmentRepository.save(appointment);

        log.info("Appointment {} cancelled by user {}", appointmentId, requestingUserId);
        notificationService.sendCancellationNotification(appointment);
    }

    // ─── Available Slots ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long doctorId, LocalDate date) {
        Doctor doctor = getDoctorById(doctorId);
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        List<DoctorSchedule> schedules = scheduleRepository
                .findByDoctorAndDayOfWeekAndActiveTrue(doctor, dayOfWeek);

        List<LocalTime> bookedTimes = appointmentRepository
                .findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date)
                .stream()
                .filter(a -> List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        AppointmentStatus.CHECKED_IN).contains(a.getStatus()))
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toList());

        List<AvailableSlotResponse> slots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            LocalTime cursor = schedule.getStartTime();
            while (cursor.plusMinutes(slotDurationMinutes).compareTo(schedule.getEndTime()) <= 0) {
                boolean available = !bookedTimes.contains(cursor);
                slots.add(new AvailableSlotResponse(cursor, available));
                cursor = cursor.plusMinutes(slotDurationMinutes);
            }
        }
        return slots;
    }

    // ─── Read Queries ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(Long patientUserId) {
        Patient patient = getPatientByUserId(patientUserId);
        return appointmentRepository
                .findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(patient)
                .stream().map(appointmentMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUpcomingAppointments(Long patientUserId) {
        Patient patient = getPatientByUserId(patientUserId);
        return appointmentRepository
                .findByPatientAndAppointmentDateGreaterThanEqualOrderByAppointmentDateAscAppointmentTimeAsc(
                        patient, LocalDate.now())
                .stream().map(appointmentMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorDailySchedule(Long doctorId, LocalDate date) {
        Doctor doctor = getDoctorById(doctorId);
        return appointmentRepository
                .findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date)
                .stream().map(appointmentMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getDoctorWeeklySchedule(Long doctorId, LocalDate weekStart) {
        Doctor doctor = getDoctorById(doctorId);
        return appointmentRepository
                .findByDoctorAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
                        doctor, weekStart, weekStart.plusDays(6))
                .stream().map(appointmentMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long appointmentId, Long requestingUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        return appointmentMapper.toResponse(appointment);
    }

    // @Override
    // @Transactional(readOnly = true)
    // public List<AppointmentResponse> getAppointmentsByDateRange(LocalDate start,
    // LocalDate end) {
    // return appointmentRepository.findByAppointmentDateBetween(start, end)
    // .stream().map(appointmentMapper::toResponse).collect(Collectors.toList());
    // }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsByDateRange(
            Long clinicId,
            LocalDate start,
            LocalDate end) {

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", clinicId));

        return appointmentRepository
                .findByClinicAndAppointmentDateBetweenOrderByAppointmentDateAscAppointmentTimeAsc(
                        clinic, start, end)
                .stream()
                .map(appointmentMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AppointmentResponse completeAppointment(Long appointmentId, Long doctorUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        // Security check: only the assigned doctor can complete the appointment
        if (!appointment.getDoctor().getUser().getId().equals(doctorUserId)) {
            throw new UnauthorizedAccessException("You are not authorized to complete this appointment.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /**
     * BR-001: Conflict check — throws AppointmentConflictException if slot taken.
     */
    private void validateSlotAvailability(Doctor doctor, LocalDate date,
                                          LocalTime time, Long excludeId) {
        if (appointmentRepository.existsConflict(doctor, date, time, excludeId)) {
            throw new AppointmentConflictException(
                    "This time slot is already booked for Dr. " +
                            doctor.getUser().getFullName() + " on " + date + " at " + time);
        }
    }

    /**
     * BR-003: Daily cap check — throws IllegalStateException if doctor is fully
     * booked.
     */
    private void validateDailyCap(Doctor doctor, LocalDate date) {
        long count = appointmentRepository.countByDoctorAndAppointmentDateAndStatusIn(
                doctor, date,
                List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED,
                        AppointmentStatus.CHECKED_IN));
        if (count >= doctor.getMaxDailyAppointments()) {
            throw new IllegalStateException("Dr. " + doctor.getUser().getFullName() +
                    " has reached the maximum appointments for " + date);
        }
    }

    private Patient getPatientByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient for user", userId));
    }

    private Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));
    }

    private Appointment getAppointmentAndVerifyOwner(Long appointmentId, Long patientUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        if (!appointment.getPatient().getUser().getId().equals(patientUserId)) {
            throw new UnauthorizedAccessException("You do not own this appointment.");
        }
        return appointment;
    }
}