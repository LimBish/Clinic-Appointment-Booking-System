package com.clinic.service.impl;

import com.clinic.dto.request.DoctorCreateRequest;
import com.clinic.dto.response.UserResponse;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.Doctor;
import com.clinic.model.entity.Patient;
import com.clinic.model.entity.User;
import com.clinic.model.enums.ClinicStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.ClinicRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.UserRepository;
import com.clinic.service.DoctorService;
import com.clinic.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final SecurityUtils SecurityUtils;
    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final PasswordEncoder passwordEncoder;

    private Doctor getCurrentDoctor() {
        Long userId = SecurityUtils.getCurrentUserId();
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Doctor not found for user"));
    }

    @Override
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    @Override
    public List<Doctor> getAllAvailableDoctors() {
        return doctorRepository.findByAvailableTrue();
    }

    @Override
    public List<Doctor> getDoctorsBySpecialization(String specialization) {
        return doctorRepository.findBySpecializationIgnoreCase(specialization);
    }

    @Override
    public Doctor getDoctorById(Long doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
    }

    @Override
    public Long getDoctorIdByUserId(Long userId) {
        return doctorRepository.findByUserId(userId)
                .map(Doctor::getId)
                .orElseThrow(() -> new RuntimeException("Doctor not found for user"));
    }

    @Override
    public List<String> getAllSpecializations() {
        return doctorRepository.findAllSpecializations();
    }

    @Override
    public UserResponse registerDoctor(DoctorCreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + req.getEmail());
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new DuplicateResourceException("Phone number is already in use.");
        }

        // Resolve and validate the chosen clinic
        Clinic clinic = clinicRepository.findById(req.getClinicId())
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", req.getClinicId()));
        if (clinic.getStatus() != ClinicStatus.ACTIVE) {
            throw new IllegalStateException("The selected clinic is not currently accepting registrations.");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.DOCTOR)
                .clinic(clinic)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        Doctor doctor = Doctor.builder()
                .user(savedUser)
                .clinic(clinic)
                .specialization(req.getSpecialization())
                .bio(req.getBio())
                .qualification(req.getQualification())
                .consultationRoom(req.getConsultationRoom())
                .maxDailyAppointments(req.getMaxDailyAppointments())
                .available(true).build();
        doctorRepository.save(doctor);

        log.info("New patient registered: {} under clinic: {}", savedUser.getEmail(), clinic.getName());
        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .build();
    }
}
