package com.clinic.repository;

import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.User;
import com.clinic.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UserRepository — Persistence Layer for User entity.
 *
 * Multi-clinic: all read queries that return lists are scoped to a Clinic.
 * The findByEmail / existsByEmail methods remain global (email is unique platform-wide).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // ─── Clinic-scoped queries (used by ADMIN) ────────────────────────────────

    List<User> findByClinic(Clinic clinic);

    List<User> findByClinicAndRole(Clinic clinic, Role role);

    List<User> findByClinicAndRoleAndEnabled(Clinic clinic, Role role, boolean enabled);

    long countByClinicAndRole(Clinic clinic, Role role);

    // ─── Platform-level queries (used by SUPER_ADMIN only) ───────────────────

    List<User> findByRole(Role role);

    /** Total registered patients across all clinics */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'PATIENT'")
    long countTotalPatients();

    /** Total registered doctors across all clinics */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'DOCTOR'")
    long countTotalDoctors();
}

