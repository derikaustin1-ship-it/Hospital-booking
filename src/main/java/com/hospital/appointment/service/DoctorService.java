package com.hospital.appointment.service;

import com.hospital.appointment.dto.DoctorProfileDto;
import com.hospital.appointment.dto.DoctorSearchFilterDto;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorService(DoctorRepository doctorRepository, UserRepository userRepository) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
    }

    public List<Doctor> getAllActiveDoctors() {
        return doctorRepository.findByAvailableTrue();
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + id));
    }

    public Doctor getDoctorByEmail(String email) {
        return doctorRepository.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found for email: " + email));
    }

    public Optional<Doctor> findDoctorByUserId(Long userId) {
        return doctorRepository.findByUserId(userId);
    }

    public List<String> getAllSpecializations() {
        return doctorRepository.findDistinctSpecializations();
    }

    public List<Doctor> searchAndFilterDoctors(DoctorSearchFilterDto filter) {
        String query = (filter != null && filter.getQuery() != null) ? filter.getQuery().trim() : null;
        String spec = (filter != null && filter.getSpecialization() != null && !filter.getSpecialization().equalsIgnoreCase("ALL"))
                ? filter.getSpecialization().trim()
                : null;

        List<Doctor> doctors = doctorRepository.filterDoctors(spec, query);

        // Sorting
        if (filter != null && filter.getSortBy() != null) {
            switch (filter.getSortBy().toLowerCase()) {
                case "experience_desc":
                    doctors.sort(Comparator.comparing(Doctor::getExperienceYears).reversed());
                    break;
                case "experience_asc":
                    doctors.sort(Comparator.comparing(Doctor::getExperienceYears));
                    break;
                case "fee_asc":
                    doctors.sort(Comparator.comparing(Doctor::getConsultationFee));
                    break;
                case "fee_desc":
                    doctors.sort(Comparator.comparing(Doctor::getConsultationFee).reversed());
                    break;
                case "name_asc":
                    doctors.sort(Comparator.comparing(d -> d.getUser().getName().toLowerCase()));
                    break;
                default:
                    // default order
                    break;
            }
        }

        return doctors;
    }

    @Transactional
    public Doctor updateDoctorProfile(String email, DoctorProfileDto dto) {
        Doctor doctor = getDoctorByEmail(email);
        User user = doctor.getUser();

        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        userRepository.save(user);

        doctor.setSpecialization(dto.getSpecialization());
        doctor.setQualification(dto.getQualification());
        doctor.setExperienceYears(dto.getExperienceYears());
        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setAbout(dto.getAbout());
        if (dto.getHospitalName() != null && !dto.getHospitalName().isBlank()) {
            doctor.setHospitalName(dto.getHospitalName());
        }
        if (dto.getClinicAddress() != null && !dto.getClinicAddress().isBlank()) {
            doctor.setClinicAddress(dto.getClinicAddress());
        }
        doctor.setAvailable(dto.isAvailable());

        return doctorRepository.save(doctor);
    }

    public DoctorProfileDto getProfileDto(Doctor doctor) {
        DoctorProfileDto dto = new DoctorProfileDto();
        dto.setId(doctor.getId());
        dto.setName(doctor.getUser().getName());
        dto.setEmail(doctor.getUser().getEmail());
        dto.setPhone(doctor.getUser().getPhone());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setQualification(doctor.getQualification());
        dto.setExperienceYears(doctor.getExperienceYears());
        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setAbout(doctor.getAbout());
        dto.setHospitalName(doctor.getHospitalName());
        dto.setClinicAddress(doctor.getClinicAddress());
        dto.setProfileImage(doctor.getProfileImage());
        dto.setAvailable(doctor.isAvailable());
        return dto;
    }
}
