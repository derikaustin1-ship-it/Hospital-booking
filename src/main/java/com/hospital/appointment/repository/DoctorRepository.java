package com.hospital.appointment.repository;

import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByUser(User user);
    Optional<Doctor> findByUserEmail(String email);
    Optional<Doctor> findByUserId(Long userId);

    List<Doctor> findByAvailableTrue();

    List<Doctor> findBySpecializationIgnoreCase(String specialization);

    @Query("SELECT DISTINCT d.specialization FROM Doctor d ORDER BY d.specialization ASC")
    List<String> findDistinctSpecializations();

    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE d.available = true AND (" +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.qualification) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Doctor> searchDoctors(@Param("query") String query);

    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE " +
           "(:specialization IS NULL OR :specialization = '' OR LOWER(d.specialization) = LOWER(:specialization)) AND " +
           "(:search IS NULL OR :search = '' OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.about) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Doctor> filterDoctors(@Param("specialization") String specialization, @Param("search") String search);
}
