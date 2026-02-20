package com.clinic.service;

import com.clinic.dto.request.ClinicCreateRequest;
import com.clinic.dto.response.ClinicResponse;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.model.entity.Clinic;
import com.clinic.model.enums.ClinicStatus;
import com.clinic.repository.*;
import com.clinic.service.impl.ClinicServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ClinicServiceTest — unit tests for SUPER_ADMIN clinic management operations.
 */
@ExtendWith(MockitoExtension.class)
class ClinicServiceTest {

    @Mock private ClinicRepository clinicRepository;
    @Mock private UserRepository userRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClinicServiceImpl clinicService;

    @Test
    @DisplayName("Should throw DuplicateResourceException when registration code already exists")
    void shouldRejectDuplicateRegistrationCode() {
        ClinicCreateRequest req = ClinicCreateRequest.builder()
                .name("Test Clinic")
                .registrationCode("KFC-001")
                .address("Kathmandu")
                .city("Kathmandu")
                .maxDoctors(10)
                .build();

        when(clinicRepository.existsByRegistrationCode("KFC-001")).thenReturn(true);

        assertThatThrownBy(() -> clinicService.createClinic(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("KFC-001");

        verify(clinicRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create clinic with PENDING status")
    void shouldCreateClinicWithPendingStatus() {
        ClinicCreateRequest req = ClinicCreateRequest.builder()
                .name("New Clinic")
                .registrationCode("NEW-001")
                .address("Lalitpur")
                .city("Lalitpur")
                .maxDoctors(5)
                .subscriptionPlan("BASIC")
                .build();

        Clinic saved = Clinic.builder()
                .id(1L).name("New Clinic")
                .registrationCode("NEW-001")
                .status(ClinicStatus.PENDING)
                .maxDoctors(5).build();

        when(clinicRepository.existsByRegistrationCode("NEW-001")).thenReturn(false);
        when(clinicRepository.existsByEmail(any())).thenReturn(false);
        when(clinicRepository.save(any(Clinic.class))).thenReturn(saved);
        when(doctorRepository.countByClinic(any())).thenReturn(0L);
        when(userRepository.countByClinicAndRole(any(), any())).thenReturn(0L);

        ClinicResponse response = clinicService.createClinic(req);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getName()).isEqualTo("New Clinic");
        verify(clinicRepository).save(any(Clinic.class));
    }

    @Test
    @DisplayName("Should activate a PENDING clinic")
    void shouldActivateClinic() {
        Clinic clinic = Clinic.builder()
                .id(1L).name("Test Clinic")
                .status(ClinicStatus.PENDING)
                .maxDoctors(10).build();

        when(clinicRepository.findById(1L)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any(Clinic.class))).thenAnswer(inv -> inv.getArgument(0));
        when(doctorRepository.countByClinic(any())).thenReturn(0L);
        when(userRepository.countByClinicAndRole(any(), any())).thenReturn(0L);

        ClinicResponse response = clinicService.activateClinic(1L);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should suspend an ACTIVE clinic with reason")
    void shouldSuspendClinic() {
        Clinic clinic = Clinic.builder()
                .id(1L).name("Active Clinic")
                .status(ClinicStatus.ACTIVE)
                .maxDoctors(10).build();

        when(clinicRepository.findById(1L)).thenReturn(Optional.of(clinic));
        when(clinicRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(doctorRepository.countByClinic(any())).thenReturn(0L);
        when(userRepository.countByClinicAndRole(any(), any())).thenReturn(0L);

        ClinicResponse response = clinicService.suspendClinic(1L, "Billing overdue");

        assertThat(response.getStatus()).isEqualTo("SUSPENDED");
        assertThat(clinic.getNotes()).contains("SUSPENDED").contains("Billing overdue");
    }

    @Test
    @DisplayName("Should throw when trying to reactivate a deactivated clinic")
    void shouldNotReactivateDeactivatedClinic() {
        Clinic clinic = Clinic.builder()
                .id(1L).name("Dead Clinic")
                .status(ClinicStatus.INACTIVE)
                .build();

        when(clinicRepository.findById(1L)).thenReturn(Optional.of(clinic));

        assertThatThrownBy(() -> clinicService.activateClinic(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivated");
    }
}
