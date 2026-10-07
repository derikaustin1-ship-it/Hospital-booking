package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByUser(User user);
    Optional<Patient> findByUserEmail(String email);
    Optional<Patient> findByUserId(Long userId);
}
