package com.clinic.service.impl;

import com.clinic.dto.request.ClinicAdminCreateRequest;
import com.clinic.dto.request.ClinicCreateRequest;
import com.clinic.dto.response.ClinicResponse;
import com.clinic.dto.response.PlatformStatsResponse;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.User;
import com.clinic.model.enums.ClinicStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.*;
import com.clinic.service.ClinicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ClinicServiceImpl — platform-level operations executed by SUPER_ADMIN.
 *
 * Data isolation guarantee:
 *   This service operates ACROSS clinics (no clinic-scope restriction).
 *   It is the only service that does so legitimately.
 *   All other services (AppointmentService, QueueService, etc.) enforce
 *   single-clinic scope via ClinicContextHolder.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClinicServiceImpl implements ClinicService {

    private final ClinicRepository clinicRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Clinic CRUD ──────────────────────────────────────────────────────────

    @Override
    public ClinicResponse createClinic(ClinicCreateRequest req) {
        if (clinicRepository.existsByRegistrationCode(req.getRegistrationCode())) {
            throw new DuplicateResourceException(
                "Registration code already in use: " + req.getRegistrationCode());
        }
        if (clinicRepository.existsByEmail(req.getContactEmail())) {
            throw new DuplicateResourceException(
                "Clinic contact email already registered: " + req.getContactEmail());
        }

        Clinic clinic = Clinic.builder()
                .name(req.getName())
                .registrationCode(req.getRegistrationCode().toUpperCase())
                .address(req.getAddress())
                .city(req.getCity())
                .phone(req.getPhone())
                .email(req.getContactEmail())
                .status(ClinicStatus.PENDING)
                .maxDoctors(req.getMaxDoctors() > 0 ? req.getMaxDoctors() : 10)
                .subscriptionPlan(req.getSubscriptionPlan() != null
                        ? req.getSubscriptionPlan() : "STANDARD")
                .notes(req.getNotes())
                .build();

        Clinic saved = clinicRepository.save(clinic);
        log.info("SUPER_ADMIN created clinic '{}' [{}]", saved.getName(), saved.getRegistrationCode());
        return toResponse(saved);
    }

    @Override
    public void assignClinicAdmin(Long clinicId, ClinicAdminCreateRequest req) {
        Clinic clinic = getClinic(clinicId);

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + req.getEmail());
        }

        User adminUser = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.ADMIN)
                .clinic(clinic)
                .enabled(true)
                .build();

        userRepository.save(adminUser);
        log.info("SUPER_ADMIN assigned admin '{}' to clinic '{}'",
                req.getEmail(), clinic.getName());
    }

    // ─── Status management ────────────────────────────────────────────────────

    @Override
    public ClinicResponse activateClinic(Long clinicId) {
        Clinic clinic = getClinic(clinicId);
        if (clinic.getStatus() == ClinicStatus.INACTIVE) {
            throw new IllegalStateException("Cannot reactivate a deactivated clinic.");
        }
        clinic.setStatus(ClinicStatus.ACTIVE);
        log.info("SUPER_ADMIN ACTIVATED clinic '{}' [{}]",
                clinic.getName(), clinic.getRegistrationCode());
        return toResponse(clinicRepository.save(clinic));
    }

    @Override
    public ClinicResponse suspendClinic(Long clinicId, String reason) {
        Clinic clinic = getClinic(clinicId);
        clinic.setStatus(ClinicStatus.SUSPENDED);
        clinic.setNotes(appendNote(clinic.getNotes(),
                "SUSPENDED: " + reason));
        log.warn("SUPER_ADMIN SUSPENDED clinic '{}' — reason: {}", clinic.getName(), reason);
        return toResponse(clinicRepository.save(clinic));
    }

    @Override
    public ClinicResponse deactivateClinic(Long clinicId, String reason) {
        Clinic clinic = getClinic(clinicId);
        clinic.setStatus(ClinicStatus.INACTIVE);
        clinic.setNotes(appendNote(clinic.getNotes(),
                "DEACTIVATED: " + reason));
        log.warn("SUPER_ADMIN DEACTIVATED clinic '{}'", clinic.getName());
        return toResponse(clinicRepository.save(clinic));
    }

    @Override
    public ClinicResponse updateClinicConfig(Long clinicId, ClinicCreateRequest req) {
        Clinic clinic = getClinic(clinicId);
        clinic.setName(req.getName());
        clinic.setAddress(req.getAddress());
        clinic.setCity(req.getCity());
        clinic.setPhone(req.getPhone());
        if (req.getMaxDoctors() > 0) clinic.setMaxDoctors(req.getMaxDoctors());
        if (req.getSubscriptionPlan() != null) clinic.setSubscriptionPlan(req.getSubscriptionPlan());
        if (req.getNotes() != null) clinic.setNotes(req.getNotes());
        return toResponse(clinicRepository.save(clinic));
    }

    // ─── Read queries ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ClinicResponse getClinicById(Long clinicId) {
        return toResponse(getClinic(clinicId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicResponse> getAllClinics() {
        return clinicRepository.findAllByOrderByNameAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClinicResponse> getClinicsByStatus(String status) {
        return clinicRepository.findByStatus(ClinicStatus.valueOf(status.toUpperCase()))
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformStatsResponse getPlatformStats() {
        long totalClinics    = clinicRepository.count();
        long activeClinics   = clinicRepository.countActiveClinics();
        long pendingClinics  = clinicRepository.countPendingClinics();
        long suspendedClinics = totalClinics - activeClinics - pendingClinics;
        long totalDoctors    = doctorRepository.count();
        long totalPatients   = userRepository.countTotalPatients();
        long totalAppts      = appointmentRepository.count();

        // Notification delivery rate (last 30 days)
        LocalDateTime since30days = LocalDateTime.now().minusDays(30);
        long totalNotifs   = notificationRepository.countTotalSince(since30days);
        long successNotifs = notificationRepository.countSuccessfulSince(since30days);
        double deliveryRate = totalNotifs > 0
                ? (double) successNotifs / totalNotifs * 100.0 : 100.0;

        return PlatformStatsResponse.builder()
                .totalClinics(totalClinics)
                .activeClinics(activeClinics)
                .pendingClinics(pendingClinics)
                .suspendedClinics(suspendedClinics)
                .totalDoctors(totalDoctors)
                .totalPatients(totalPatients)
                .totalAppointments(totalAppts)
                .notificationDeliveryRatePercent(deliveryRate)
                .build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Clinic getClinic(Long id) {
        return clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", id));
    }

    private ClinicResponse toResponse(Clinic c) {
        return ClinicResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .registrationCode(c.getRegistrationCode())
                .address(c.getAddress())
                .city(c.getCity())
                .phone(c.getPhone())
                .email(c.getEmail())
                .status(c.getStatus().name())
                .maxDoctors(c.getMaxDoctors())
                .subscriptionPlan(c.getSubscriptionPlan())
                .notes(c.getNotes())
                .totalDoctors((int) doctorRepository.countByClinic(c))
                .totalPatients((int) userRepository.countByClinicAndRole(c, Role.PATIENT))
                .createdAt(c.getCreatedAt())
                .build();
    }

    private String appendNote(String existing, String newNote) {
        String timestamp = "[" + LocalDateTime.now().toString().substring(0, 16) + "] ";
        return (existing == null ? "" : existing + "\n") + timestamp + newNote;
    }
}
