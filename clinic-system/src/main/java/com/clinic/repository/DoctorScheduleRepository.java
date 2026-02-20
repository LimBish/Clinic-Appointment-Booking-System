package com.clinic.repository;

import com.clinic.model.entity.Doctor;
import com.clinic.model.entity.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorAndActiveTrue(Doctor doctor);

    List<DoctorSchedule> findByDoctorAndDayOfWeekAndActiveTrue(Doctor doctor, DayOfWeek day);

    void deleteByDoctor(Doctor doctor);
}
