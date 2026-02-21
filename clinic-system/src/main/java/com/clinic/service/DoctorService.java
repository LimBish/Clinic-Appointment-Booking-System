package com.clinic.service;

import com.clinic.dto.request.DoctorCreateRequest;
import com.clinic.dto.request.PatientRegistrationRequest;
import com.clinic.dto.response.AppointmentResponse;
import com.clinic.dto.response.UserResponse;
import com.clinic.model.entity.Doctor;

import java.util.List;

public interface DoctorService {
    List<Doctor> getAllDoctors();
    List<Doctor> getAllAvailableDoctors();
    List<Doctor> getDoctorsBySpecialization(String specialization);
    Doctor getDoctorById(Long doctorId);
    Long getDoctorIdByUserId(Long userId);
    List<String> getAllSpecializations();
    UserResponse registerDoctor(DoctorCreateRequest request);
}
