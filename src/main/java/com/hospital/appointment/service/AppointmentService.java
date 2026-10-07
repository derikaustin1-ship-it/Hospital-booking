package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentBookingRequest;
import com.hospital.appointment.dto.TimeSlotDto;
import com.hospital.appointment.entity.*;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.AvailabilityRepository;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AvailabilityService availabilityService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorRepository doctorRepository,
                              PatientRepository patientRepository,
                              AvailabilityService availabilityService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.availabilityService = availabilityService;
    }

    @Transactional
    public Appointment bookAppointment(Patient patient, AppointmentBookingRequest request) {
        // 1. Validate Doctor
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Selected doctor not found."));

        if (!doctor.isAvailable()) {
            throw new IllegalArgumentException("Doctor " + doctor.getUser().getName() + " is currently not accepting appointments.");
        }

        // 2. Validate Date
        LocalDate date = request.getAppointmentDate();
        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book appointments for past dates.");
        }

        LocalTime time = request.getAppointmentTime();
        if (date.equals(LocalDate.now()) && time.isBefore(LocalTime.now())) {
            throw new IllegalArgumentException("Cannot book appointments for time slots that have already passed today.");
        }

        // 3. Validate Doctor Availability Schedule for this day and slot
        List<TimeSlotDto> availableSlots = availabilityService.getAvailableTimeSlots(doctor.getId(), date);
        boolean slotExistsAndAvailable = availableSlots.stream()
                .anyMatch(s -> s.getStartTime().equals(time) && s.isAvailable());

        if (!slotExistsAndAvailable) {
            throw new IllegalArgumentException("The selected time slot " + time + " is not available for booking.");
        }

        // 4. Prevent Double Booking
        boolean exists = appointmentRepository.existsActiveAppointmentAtDateTime(doctor.getId(), date, time);
        if (exists) {
            throw new IllegalArgumentException("This appointment slot has just been booked by another patient. Please choose a different slot.");
        }

        // 5. Create Appointment
        Appointment appointment = new Appointment(
                patient,
                doctor,
                date,
                time,
                request.getReason(),
                request.getNotes()
        );
        appointment.setStatus(AppointmentStatus.PENDING);

        return appointmentRepository.save(appointment);
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with id: " + id));
    }

    @Transactional
    public Appointment approveAppointment(Long appointmentId, Long doctorId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Unauthorized: This appointment does not belong to your schedule.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING appointments can be approved. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.APPROVED);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment rejectAppointment(Long appointmentId, Long doctorId, String reason) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Unauthorized: This appointment does not belong to your schedule.");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING appointments can be rejected. Current status: " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Doctor unavailable at this time");
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment completeAppointment(Long appointmentId, Long doctorId, String remarks) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Unauthorized: This appointment does not belong to your schedule.");
        }

        if (appointment.getStatus() != AppointmentStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED appointments can be marked as COMPLETED.");
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        if (remarks != null && !remarks.isBlank()) {
            appointment.setDoctorRemarks(remarks);
        }
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public Appointment cancelAppointmentByPatient(Long appointmentId, Long patientId, String reason) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new SecurityException("Unauthorized: You do not own this appointment.");
        }

        if (!appointment.isCancellable()) {
            if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
                throw new IllegalStateException("Completed appointments cannot be cancelled.");
            }
            if (appointment.getStatus() == AppointmentStatus.CANCELLED || appointment.getStatus() == AppointmentStatus.REJECTED) {
                throw new IllegalStateException("Appointment is already " + appointment.getStatus());
            }
            throw new IllegalStateException("Appointments can only be cancelled at least 2 hours before the scheduled appointment time.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(reason != null && !reason.isBlank() ? reason : "Cancelled by patient");
        return appointmentRepository.save(appointment);
    }

    // Patient Appointment Lists and Filtering
    public List<Appointment> getPatientAppointments(Long patientId, String filter) {
        List<Appointment> list = appointmentRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);

        if (filter == null || filter.equalsIgnoreCase("ALL")) {
            return list;
        }

        LocalDate today = LocalDate.now();
        switch (filter.toUpperCase()) {
            case "UPCOMING":
                return list.stream()
                        .filter(a -> (a.getAppointmentDate().isAfter(today) ||
                                     (a.getAppointmentDate().isEqual(today) && a.getAppointmentTime().isAfter(LocalTime.now()))) &&
                                     (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.APPROVED))
                        .collect(Collectors.toList());
            case "PENDING":
                return list.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).collect(Collectors.toList());
            case "APPROVED":
                return list.stream().filter(a -> a.getStatus() == AppointmentStatus.APPROVED).collect(Collectors.toList());
            case "COMPLETED":
                return list.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).collect(Collectors.toList());
            case "CANCELLED":
                return list.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.REJECTED).collect(Collectors.toList());
            default:
                return list;
        }
    }

    // Doctor Appointment Lists and Filtering
    public List<Appointment> getDoctorAppointments(Long doctorId, String filter) {
        List<Appointment> list = appointmentRepository.findByDoctorIdOrderByAppointmentDateDescAppointmentTimeDesc(doctorId);

        if (filter == null || filter.equalsIgnoreCase("ALL")) {
            return list;
        }

        LocalDate today = LocalDate.now();
        switch (filter.toUpperCase()) {
            case "TODAY":
                return list.stream()
                        .filter(a -> a.getAppointmentDate().isEqual(today) && a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.REJECTED)
                        .sorted(Comparator.comparing(Appointment::getAppointmentTime))
                        .collect(Collectors.toList());
            case "UPCOMING":
                return list.stream()
                        .filter(a -> a.getAppointmentDate().isAfter(today) && (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.APPROVED))
                        .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                        .collect(Collectors.toList());
            case "PENDING":
                return list.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.PENDING)
                        .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                        .collect(Collectors.toList());
            case "APPROVED":
                return list.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.APPROVED)
                        .sorted(Comparator.comparing(Appointment::getAppointmentDate).thenComparing(Appointment::getAppointmentTime))
                        .collect(Collectors.toList());
            case "COMPLETED":
                return list.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                        .collect(Collectors.toList());
            case "CANCELLED":
                return list.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.REJECTED)
                        .collect(Collectors.toList());
            default:
                return list;
        }
    }

    // Patient Dashboard Statistics
    public Map<String, Object> getPatientDashboardStats(Long patientId) {
        Map<String, Object> stats = new HashMap<>();
        long total = appointmentRepository.countByPatientId(patientId);
        long upcoming = appointmentRepository.countUpcomingByPatientId(patientId, LocalDate.now());
        long pending = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.PENDING);
        long completed = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.COMPLETED);
        long cancelled = appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.CANCELLED) +
                         appointmentRepository.countByPatientIdAndStatus(patientId, AppointmentStatus.REJECTED);

        stats.put("total", total);
        stats.put("upcoming", upcoming);
        stats.put("pending", pending);
        stats.put("completed", completed);
        stats.put("cancelled", cancelled);
        return stats;
    }

    // Doctor Dashboard Statistics
    public Map<String, Object> getDoctorDashboardStats(Long doctorId) {
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();

        long todayCount = appointmentRepository.countTodayByDoctorId(doctorId, today);
        long upcomingCount = appointmentRepository.countUpcomingByDoctorId(doctorId, today);
        long pendingCount = appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.PENDING);
        long completedCount = appointmentRepository.countByDoctorIdAndStatus(doctorId, AppointmentStatus.COMPLETED);
        long totalCount = appointmentRepository.countByDoctorId(doctorId);

        stats.put("todayCount", todayCount);
        stats.put("upcomingCount", upcomingCount);
        stats.put("pendingCount", pendingCount);
        stats.put("completedCount", completedCount);
        stats.put("totalCount", totalCount);
        return stats;
    }

    public List<Patient> getPatientsTreatedByDoctor(Long doctorId) {
        return appointmentRepository.findDistinctPatientsByDoctorId(doctorId);
    }
}
