package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AppointmentBookingRequest;
import com.hospital.appointment.dto.DoctorSearchFilterDto;
import com.hospital.appointment.dto.TimeSlotDto;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.Availability;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.service.AvailabilityService;
import com.hospital.appointment.service.DoctorService;
import com.hospital.appointment.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AppointmentApiController {

    private final DoctorService doctorService;
    private final AvailabilityService availabilityService;
    private final AppointmentService appointmentService;
    private final PatientService patientService;

    public AppointmentApiController(DoctorService doctorService,
                                    AvailabilityService availabilityService,
                                    AppointmentService appointmentService,
                                    PatientService patientService) {
        this.doctorService = doctorService;
        this.availabilityService = availabilityService;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<Doctor>> getAllDoctors(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "specialization", required = false) String specialization,
            @RequestParam(name = "sortBy", required = false) String sortBy) {

        DoctorSearchFilterDto filter = new DoctorSearchFilterDto(query, specialization, sortBy);
        List<Doctor> doctors = doctorService.searchAndFilterDoctors(filter);
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @GetMapping("/doctors/{id}/availability")
    public ResponseEntity<List<Availability>> getDoctorAvailability(@PathVariable("id") Long id) {
        return ResponseEntity.ok(availabilityService.getDoctorAvailabilities(id));
    }

    @GetMapping("/doctors/{id}/slots")
    public ResponseEntity<List<TimeSlotDto>> getAvailableSlots(
            @PathVariable("id") Long id,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TimeSlotDto> slots = availabilityService.getAvailableTimeSlots(id, date);
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/appointments")
    public ResponseEntity<?> bookAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AppointmentBookingRequest request) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            Patient patient = patientService.getPatientByEmail(userDetails.getUsername());
            Appointment appointment = appointmentService.bookAppointment(patient, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/appointments")
    public ResponseEntity<?> getUserAppointments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "filter", required = false, defaultValue = "ALL") String filter) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            // Check if patient
            try {
                Patient patient = patientService.getPatientByEmail(userDetails.getUsername());
                return ResponseEntity.ok(appointmentService.getPatientAppointments(patient.getId(), filter));
            } catch (Exception ignored) {
                Doctor doctor = doctorService.getDoctorByEmail(userDetails.getUsername());
                return ResponseEntity.ok(appointmentService.getDoctorAppointments(doctor.getId(), filter));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<?> cancelAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false) String reason) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            Patient patient = patientService.getPatientByEmail(userDetails.getUsername());
            Appointment appointment = appointmentService.cancelAppointmentByPatient(id, patient.getId(), reason);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
