package com.clinic.service.impl;

import com.clinic.dto.request.PatientRegistrationRequest;
import com.clinic.dto.response.UserResponse;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.model.entity.Patient;
import com.clinic.model.entity.User;
import com.clinic.model.enums.Role;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.UserRepository;
import com.clinic.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse registerPatient(PatientRegistrationRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + req.getEmail());
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new DuplicateResourceException("Phone number is already in use.");
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(Role.PATIENT)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        Patient patient = Patient.builder()
                .user(savedUser)
                .dateOfBirth(req.getDateOfBirth())
                .gender(req.getGender())
                .address(req.getAddress())
                .emailConsent(req.isEmailConsent())
                .smsConsent(req.isSmsConsent())
                .build();
        patientRepository.save(patient);

        log.info("New patient registered: {}", savedUser.getEmail());
        return toResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserProfile(Long userId) {
        return toResponse(findUser(userId));
    }

    @Override
    public UserResponse updateProfile(Long userId, PatientRegistrationRequest req) {
        User user = findUser(userId);
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        return toResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void toggleUserEnabled(Long userId) {
        User user = findUser(userId);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User {} enabled={}", userId, user.isEnabled());
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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

    @Override
    public Long getClinicIdByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getClinic() == null) {
            throw new IllegalStateException("User is not associated with a clinic.");
        }

        return user.getClinic().getId();
    }

}
