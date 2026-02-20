package com.clinic.service;

import com.clinic.dto.request.AppointmentBookRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.exception.AppointmentConflictException;
import com.clinic.model.entity.*;
import com.clinic.model.enums.AppointmentStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.*;
import com.clinic.service.impl.AppointmentServiceImpl;
import com.clinic.util.AppointmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AppointmentServiceTest — unit tests for the business layer.
 *
 * Tests use Mockito to mock all repository and external dependencies.
 * This isolates the business logic from the database (pure unit tests).
 *
 * Key test scenarios:
 *  1. Successful appointment booking
 *  2. BR-001: Double booking prevention
 *  3. BR-003: Daily cap enforcement
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private DoctorScheduleRepository scheduleRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AppointmentMapper appointmentMapper;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private User patientUser;
    private Patient patient;
    private User doctorUser;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        patientUser = User.builder().id(1L).fullName("Ram Sharma")
                .email("ram@test.com").role(Role.PATIENT).build();
        patient = Patient.builder().id(1L).user(patientUser).emailConsent(true).build();

        doctorUser = User.builder().id(2L).fullName("Dr. Priya Singh")
                .email("priya@clinic.com").role(Role.DOCTOR).build();
        doctor = Doctor.builder().id(1L).user(doctorUser)
                .specialization("General Physician").maxDailyAppointments(20).build();
    }

    @Test
    @DisplayName("BR-001: Should throw AppointmentConflictException when slot is already booked")
    void shouldThrowConflictWhenSlotTaken() {
        // Given
        AppointmentBookRequest request = AppointmentBookRequest.builder()
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(10, 0))
                .reason("General checkup")
                .build();

        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsConflict(
                eq(doctor), any(LocalDate.class), any(LocalTime.class), isNull()))
                .thenReturn(true); // slot already taken

        // When / Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(1L, request))
                .isInstanceOf(AppointmentConflictException.class)
                .hasMessageContaining("already booked");

        verify(appointmentRepository, never()).save(any());
        verify(notificationService, never()).sendAppointmentConfirmation(any());
    }

    @Test
    @DisplayName("BR-003: Should throw exception when doctor's daily cap is exceeded")
    void shouldThrowWhenDailyCapExceeded() {
        AppointmentBookRequest request = AppointmentBookRequest.builder()
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(11, 0))
                .build();

        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsConflict(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.countByDoctorAndAppointmentDateAndStatusIn(
                any(), any(), any())).thenReturn(20L); // at max capacity

        assertThatThrownBy(() -> appointmentService.bookAppointment(1L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("maximum appointments");
    }

    @Test
    @DisplayName("Should successfully book appointment when slot is available")
    void shouldBookAppointmentSuccessfully() {
        AppointmentBookRequest request = AppointmentBookRequest.builder()
                .doctorId(1L)
                .appointmentDate(LocalDate.now().plusDays(1))
                .appointmentTime(LocalTime.of(10, 0))
                .reason("Annual checkup")
                .build();

        Appointment savedAppt = Appointment.builder()
                .id(1L).patient(patient).doctor(doctor)
                .appointmentDate(request.getAppointmentDate())
                .appointmentTime(request.getAppointmentTime())
                .status(AppointmentStatus.CONFIRMED).build();

        AppointmentResponse expectedResponse = AppointmentResponse.builder()
                .id(1L).status("CONFIRMED").build();

        when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentRepository.existsConflict(any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.countByDoctorAndAppointmentDateAndStatusIn(any(), any(), any()))
                .thenReturn(5L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppt);
        when(appointmentMapper.toResponse(savedAppt)).thenReturn(expectedResponse);

        AppointmentResponse result = appointmentService.bookAppointment(1L, request);

        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        verify(appointmentRepository).save(any(Appointment.class));
        verify(notificationService).sendAppointmentConfirmation(savedAppt);
    }
}
