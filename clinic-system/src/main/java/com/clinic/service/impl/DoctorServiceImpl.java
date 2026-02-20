package com.clinic.service.impl;

import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.Doctor;
import com.clinic.repository.ClinicRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.service.DoctorService;
import com.clinic.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;

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
}
