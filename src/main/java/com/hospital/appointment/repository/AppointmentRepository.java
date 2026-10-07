package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.AppointmentStatus;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(Patient patient);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId);

    List<Appointment> findByDoctorOrderByAppointmentDateDescAppointmentTimeDesc(Doctor doctor);

    List<Appointment> findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(Long doctorId);

    List<Appointment> findByDoctorIdAndAppointmentDateOrderByAppointmentTimeAsc(Long doctorId, LocalDate appointmentDate);

    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateAscAppointmentTimeAsc(Long doctorId, AppointmentStatus status);

    List<Appointment> findByPatientIdAndStatusOrderByAppointmentDateDescAppointmentTimeDesc(Long patientId, AppointmentStatus status);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    List<Appointment> findActiveAppointmentsByDoctorAndDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a WHERE " +
           "a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.appointmentTime = :time AND " +
           "a.status NOT IN ('CANCELLED', 'REJECTED')")
    boolean existsActiveAppointmentAtDateTime(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :date AND a.appointmentTime = :time AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    Optional<Appointment> findActiveAppointmentByDoctorDateTime(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    // Patient metrics
    long countByPatientId(Long patientId);
    long countByPatientIdAndStatus(Long patientId, AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.appointmentDate >= :today AND a.status IN ('PENDING', 'APPROVED')")
    long countUpcomingByPatientId(@Param("patientId") Long patientId, @Param("today") LocalDate today);

    // Doctor metrics
    long countByDoctorId(Long doctorId);
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate = :today AND a.status NOT IN ('CANCELLED', 'REJECTED')")
    long countTodayByDoctorId(@Param("doctorId") Long doctorId, @Param("today") LocalDate today);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate > :today AND a.status IN ('PENDING', 'APPROVED')")
    long countUpcomingByDoctorId(@Param("doctorId") Long doctorId, @Param("today") LocalDate today);

    @Query("SELECT DISTINCT a.patient FROM Appointment a WHERE a.doctor.id = :doctorId")
    List<Patient> findDistinctPatientsByDoctorId(@Param("doctorId") Long doctorId);
}
