package com.hospital.appointment.controller;

import com.hospital.appointment.dto.*;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.entity.Gender;
import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.service.AuthService;
import com.hospital.appointment.service.DoctorService;
import com.hospital.appointment.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/patient")
public class PatientController {

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final AuthService authService;

    public PatientController(PatientService patientService,
                             DoctorService doctorService,
                             AppointmentService appointmentService,
                             AuthService authService) {
        this.patientService = patientService;
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.authService = authService;
    }

    private Patient getCurrentPatient(UserDetails userDetails) {
        return patientService.getPatientByEmail(userDetails.getUsername());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Patient patient = getCurrentPatient(userDetails);
        Map<String, Object> stats = appointmentService.getPatientDashboardStats(patient.getId());
        List<Appointment> upcomingAppointments = appointmentService.getPatientAppointments(patient.getId(), "UPCOMING");

        model.addAttribute("patient", patient);
        model.addAttribute("stats", stats);
        model.addAttribute("upcomingAppointments", upcomingAppointments);
        return "patient/dashboard";
    }

    @GetMapping("/doctors")
    public String doctors(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "specialization", required = false) String specialization,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            Model model) {

        DoctorSearchFilterDto filter = new DoctorSearchFilterDto(query, specialization, sortBy);
        List<Doctor> doctors = doctorService.searchAndFilterDoctors(filter);

        model.addAttribute("doctors", doctors);
        model.addAttribute("specializations", doctorService.getAllSpecializations());
        model.addAttribute("currentFilter", filter);
        return "patient/doctors";
    }

    @GetMapping("/doctor/{id}")
    public String doctorDetails(@PathVariable("id") Long id, Model model) {
        Doctor doctor = doctorService.getDoctorById(id);
        model.addAttribute("doctor", doctor);
        return "patient/doctor-detail";
    }

    @GetMapping("/appointment/book")
    public String bookAppointmentPage(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            Model model) {

        AppointmentBookingRequest bookingRequest = new AppointmentBookingRequest();
        if (doctorId != null) {
            bookingRequest.setDoctorId(doctorId);
        }
        bookingRequest.setAppointmentDate(LocalDate.now());

        model.addAttribute("bookingRequest", bookingRequest);
        model.addAttribute("doctors", doctorService.getAllActiveDoctors());
        model.addAttribute("selectedDoctorId", doctorId);
        return "patient/book-appointment";
    }

    @PostMapping("/appointment/book")
    public String handleBookAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("bookingRequest") AppointmentBookingRequest bookingRequest,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Patient patient = getCurrentPatient(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("doctors", doctorService.getAllActiveDoctors());
            return "patient/book-appointment";
        }

        try {
            appointmentService.bookAppointment(patient, bookingRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Your appointment request has been submitted successfully! The doctor will review and approve it.");
            return "redirect:/patient/appointments";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("doctors", doctorService.getAllActiveDoctors());
            return "patient/book-appointment";
        }
    }

    @GetMapping("/appointments")
    public String myAppointments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "filter", required = false, defaultValue = "ALL") String filter,
            Model model) {

        Patient patient = getCurrentPatient(userDetails);
        List<Appointment> appointments = appointmentService.getPatientAppointments(patient.getId(), filter);

        model.addAttribute("appointments", appointments);
        model.addAttribute("currentFilter", filter.toUpperCase());
        model.addAttribute("patient", patient);
        return "patient/appointments";
    }

    @PostMapping("/appointment/{id}/cancel")
    public String cancelAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "cancellationReason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        Patient patient = getCurrentPatient(userDetails);

        try {
            appointmentService.cancelAppointmentByPatient(id, patient.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " has been cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }

        return "redirect:/patient/appointments";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Patient patient = getCurrentPatient(userDetails);
        PatientProfileDto profileDto = patientService.getProfileDto(patient);

        model.addAttribute("profileDto", profileDto);
        model.addAttribute("patient", patient);
        model.addAttribute("genders", Gender.values());
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "patient/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("profileDto") PatientProfileDto profileDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Patient patient = getCurrentPatient(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("patient", patient);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            return "patient/profile";
        }

        try {
            patientService.updatePatientProfile(userDetails.getUsername(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Your profile details have been updated successfully!");
            return "redirect:/patient/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("patient", patient);
            model.addAttribute("genders", Gender.values());
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            return "patient/profile";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Patient patient = getCurrentPatient(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("patient", patient);
            model.addAttribute("profileDto", patientService.getProfileDto(patient));
            model.addAttribute("genders", Gender.values());
            return "patient/profile";
        }

        try {
            authService.changePassword(userDetails.getUsername(), changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully!");
            return "redirect:/patient/profile";
        } catch (Exception e) {
            model.addAttribute("passwordErrorMessage", e.getMessage());
            model.addAttribute("patient", patient);
            model.addAttribute("profileDto", patientService.getProfileDto(patient));
            model.addAttribute("genders", Gender.values());
            return "patient/profile";
        }
    }
}
