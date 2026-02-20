package com.clinic.service;

import com.clinic.dto.request.PatientRegistrationRequest;
import com.clinic.dto.response.UserResponse;

public interface UserService {
    UserResponse registerPatient(PatientRegistrationRequest request);
    UserResponse getUserProfile(Long userId);
    UserResponse updateProfile(Long userId, PatientRegistrationRequest request);
    void changePassword(Long userId, String currentPassword, String newPassword);
    void toggleUserEnabled(Long userId);   // Admin: suspend / re-enable account
    Long getClinicIdByUserId(Long userId);
}
