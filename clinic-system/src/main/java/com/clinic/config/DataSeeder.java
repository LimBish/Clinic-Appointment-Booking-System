package com.clinic.config;

import com.clinic.model.entity.Clinic;
import com.clinic.model.entity.Doctor;
import com.clinic.model.entity.DoctorSchedule;
import com.clinic.model.entity.User;
import com.clinic.model.enums.ClinicStatus;
import com.clinic.model.enums.Role;
import com.clinic.repository.ClinicRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.DoctorScheduleRepository;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DataSeeder — seeds bootstrap data on first startup.
 *
 * Multi-clinic seed plan:
 *  1. SUPER_ADMIN account  (admin / admin)
 *     — No clinic affiliation
 *
 *  2. Demo Clinic A "Kathmandu Family Clinic" [KFC-001]
 *     → Admin:  admin.kfc@clinic.com  / Admin@1234
 *     → Doctor: dr.priya@kfc.com      / Doctor@1234
 *
 *  3. Demo Clinic B "Pokhara Health Centre" [PHC-002]
 *     → Admin:  admin.phc@clinic.com  / Admin@1234
 *     → Doctor: dr.arjun@phc.com      / Doctor@1234
 *
 * Idempotent — checks existence before inserting.
 * Remove or disable in production after initial setup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ClinicRepository clinicRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        Clinic clinicA = seedClinicA();
        Clinic clinicB = seedClinicB();
        if (clinicA != null) seedClinicADoctor(clinicA);
        if (clinicB != null) seedClinicBDoctor(clinicB);
        log.info("✅ DataSeeder completed — system ready.");
        log.info("──────────────────────────────────────────────────────");
        log.info("  SUPER_ADMIN:    admin / admin");
        log.info("  CLINIC A ADMIN: admin.kfc@clinic.com    / Admin@1234");
        log.info("  CLINIC B ADMIN: admin.phc@clinic.com    / Admin@1234");
        log.info("──────────────────────────────────────────────────────");
    }

    // ─── 1. Super Admin (no clinic) ──────────────────────────────────────────

    private void seedSuperAdmin() {
        if (!userRepository.existsByEmail("admin@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("Platform Super Admin")
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("admin"))
                    .phone("+977 9800000000")
                    .role(Role.SUPER_ADMIN)
                    .clinic(null)   // SUPER_ADMIN has no clinic affiliation
                    .enabled(true)
                    .build());
            log.info("Seeded SUPER_ADMIN: admin");
        }
    }

    // ─── 2. Clinic A — Kathmandu Family Clinic ────────────────────────────────

    private Clinic seedClinicA() {
        if (clinicRepository.existsByRegistrationCode("KFC-001")) return
                clinicRepository.findByRegistrationCode("KFC-001").orElse(null);

        Clinic clinic = clinicRepository.save(Clinic.builder()
                .name("Kathmandu Family Clinic")
                .registrationCode("KFC-001")
                .address("New Baneshwor, Kathmandu")
                .city("Kathmandu")
                .phone("+977 01-4567890")
                .email("info@kfc.clinic")
                .status(ClinicStatus.ACTIVE)
                .maxDoctors(15)
                .subscriptionPlan("STANDARD")
                .build());

        // Admin for Clinic A
        if (!userRepository.existsByEmail("admin.kfc@clinic.com")) {
            userRepository.save(User.builder()
                    .fullName("KFC Clinic Admin")
                    .email("admin.kfc@clinic.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .phone("+977 9800000001")
                    .role(Role.ADMIN)
                    .clinic(clinic)
                    .enabled(true)
                    .build());
        }
        log.info("Seeded Clinic A: Kathmandu Family Clinic [KFC-001]");
        return clinic;
    }

    // ─── 3. Clinic B — Pokhara Health Centre ─────────────────────────────────

    private Clinic seedClinicB() {
        if (clinicRepository.existsByRegistrationCode("PHC-002")) return
                clinicRepository.findByRegistrationCode("PHC-002").orElse(null);

        Clinic clinic = clinicRepository.save(Clinic.builder()
                .name("Pokhara Health Centre")
                .registrationCode("PHC-002")
                .address("Lakeside, Pokhara")
                .city("Pokhara")
                .phone("+977 061-123456")
                .email("info@phc.clinic")
                .status(ClinicStatus.ACTIVE)
                .maxDoctors(10)
                .subscriptionPlan("BASIC")
                .build());

        // Admin for Clinic B
        if (!userRepository.existsByEmail("admin.phc@clinic.com")) {
            userRepository.save(User.builder()
                    .fullName("PHC Clinic Admin")
                    .email("admin.phc@clinic.com")
                    .password(passwordEncoder.encode("Admin@1234"))
                    .phone("+977 9800000002")
                    .role(Role.ADMIN)
                    .clinic(clinic)
                    .enabled(true)
                    .build());
        }
        log.info("Seeded Clinic B: Pokhara Health Centre [PHC-002]");
        return clinic;
    }

    // ─── Doctor for Clinic A ──────────────────────────────────────────────────

    private void seedClinicADoctor(Clinic clinic) {
        if (userRepository.existsByEmail("dr.priya@kfc.com")) return;

        User drUser = userRepository.save(User.builder()
                .fullName("Priya Singh")
                .email("dr.priya@kfc.com")
                .password(passwordEncoder.encode("Doctor@1234"))
                .phone("+977 9800000003")
                .role(Role.DOCTOR)
                .clinic(clinic)
                .enabled(true)
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(drUser)
                .clinic(clinic)
                .specialization("General Physician")
                .qualification("MBBS, MD")
                .consultationRoom("Room 101")
                .maxDailyAppointments(20)
                .available(true)
                .build());

        addWeekdaySchedule(doctor, LocalTime.of(9, 0), LocalTime.of(13, 0));
        addWeekdaySchedule(doctor, LocalTime.of(14, 0), LocalTime.of(17, 0));
        log.info("Seeded Doctor (Clinic A): dr.priya@kfc.com");
    }

    // ─── Doctor for Clinic B ──────────────────────────────────────────────────

    private void seedClinicBDoctor(Clinic clinic) {
        if (userRepository.existsByEmail("dr.arjun@phc.com")) return;

        User drUser = userRepository.save(User.builder()
                .fullName("Arjun Thapa")
                .email("dr.arjun@phc.com")
                .password(passwordEncoder.encode("Doctor@1234"))
                .phone("+977 9800000004")
                .role(Role.DOCTOR)
                .clinic(clinic)
                .enabled(true)
                .build());

        Doctor doctor = doctorRepository.save(Doctor.builder()
                .user(drUser)
                .clinic(clinic)
                .specialization("Cardiologist")
                .qualification("MBBS, MD (Cardiology)")
                .consultationRoom("Room 202")
                .maxDailyAppointments(15)
                .available(true)
                .build());

        for (DayOfWeek day : new DayOfWeek[]{DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY}) {
            scheduleRepository.save(DoctorSchedule.builder()
                    .doctor(doctor).dayOfWeek(day)
                    .startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(16, 0))
                    .active(true).build());
        }
        log.info("Seeded Doctor (Clinic B): dr.arjun@phc.com");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void addWeekdaySchedule(Doctor doctor, LocalTime start, LocalTime end) {
        for (DayOfWeek day : new DayOfWeek[]{
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY}) {
            scheduleRepository.save(DoctorSchedule.builder()
                    .doctor(doctor).dayOfWeek(day)
                    .startTime(start).endTime(end)
                    .active(true).build());
        }
    }
}

