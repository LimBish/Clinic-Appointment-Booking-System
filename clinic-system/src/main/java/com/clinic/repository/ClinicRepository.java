package com.clinic.repository;

import com.clinic.model.entity.Clinic;
import com.clinic.model.enums.ClinicStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ClinicRepository — Persistence Layer for Clinic (tenant) entity.
 *
 * Used exclusively by SUPER_ADMIN and ClinicService.
 * Clinic-scoped admins never call these methods — they operate
 * within their clinic via clinic-scoped service methods.
 */
@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    Optional<Clinic> findByRegistrationCode(String registrationCode);

    boolean existsByRegistrationCode(String code);

    boolean existsByEmail(String email);

    @Query("SELECT c FROM Clinic c WHERE c.status = 'ACTIVE'")
    List<Clinic> findByStatus(ClinicStatus status);

    List<Clinic> findAllByOrderByNameAsc();

    /** Count of doctors currently active under each clinic — used in dashboard */
    @Query("""
            SELECT c.id, COUNT(d.id)
            FROM Clinic c LEFT JOIN c.doctors d
            WHERE d.available = true OR d IS NULL
            GROUP BY c.id
            """)
    List<Object[]> countActiveDoctorsPerClinic();

    /** Platform-level stats for super admin overview */
    @Query("SELECT COUNT(c) FROM Clinic c WHERE c.status = 'ACTIVE'")
    long countActiveClinics();

    @Query("SELECT COUNT(c) FROM Clinic c WHERE c.status = 'PENDING'")
    long countPendingClinics();
}
