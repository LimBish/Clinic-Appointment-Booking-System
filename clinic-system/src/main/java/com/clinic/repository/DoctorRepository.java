package com.clinic.repository;

import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DoctorRepository — Persistence Layer for Doctor entity.
 *
 * Multi-clinic: all list queries scoped by Clinic.
 * SUPER_ADMIN uses platform-level queries (no clinic filter).
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    // ─── Clinic-scoped (ADMIN / PATIENT use) ─────────────────────────────────

    List<Doctor> findByClinicAndAvailableTrue(Clinic clinic);

    List<Doctor> findByClinic(Clinic clinic);

    List<Doctor> findByClinicAndSpecialization(Clinic clinic, String specialization);

    Optional<Doctor> findByUserId(Long userId);

    @Query("SELECT DISTINCT d.specialization FROM Doctor d WHERE d.clinic = :clinic ORDER BY d.specialization")
    List<String> findSpecializationsByClinic(@Param("clinic") Clinic clinic);

    long countByClinic(Clinic clinic);

    // ─── Platform-level (SUPER_ADMIN) ─────────────────────────────────────────

    @Query("SELECT DISTINCT d.specialization FROM Doctor d ORDER BY d.specialization")
    List<String> findAllSpecializations();

    List<Doctor> findByAvailableTrue();

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    long countByAvailableTrue();
}
