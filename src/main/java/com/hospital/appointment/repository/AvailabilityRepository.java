package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Availability;
import com.hospital.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByDoctor(Doctor doctor);
    List<Availability> findByDoctorId(Long doctorId);
    List<Availability> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);

    @Query("SELECT a FROM Availability a WHERE a.doctor.id = :doctorId AND a.dayOfWeek = :dayOfWeek AND " +
           "((:startTime >= a.startTime AND :startTime < a.endTime) OR " +
           "(:endTime > a.startTime AND :endTime <= a.endTime) OR " +
           "(:startTime <= a.startTime AND :endTime >= a.endTime))")
    List<Availability> findOverlappingAvailabilities(
            @Param("doctorId") Long doctorId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);
}
