package com.hospital.appointment.controller;

import com.hospital.appointment.dto.AvailabilityDto;
import com.hospital.appointment.dto.ChangePasswordDto;
import com.hospital.appointment.dto.DoctorProfileDto;
import com.hospital.appointment.entity.*;
import com.hospital.appointment.service.AppointmentService;
import com.hospital.appointment.service.AuthService;
import com.hospital.appointment.service.AvailabilityService;
import com.hospital.appointment.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/doctor")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final AvailabilityService availabilityService;
    private final AuthService authService;

    public DoctorController(DoctorService doctorService,
                            AppointmentService appointmentService,
                            AvailabilityService availabilityService,
                            AuthService authService) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.availabilityService = availabilityService;
        this.authService = authService;
    }

    private Doctor getCurrentDoctor(UserDetails userDetails) {
        return doctorService.getDoctorByEmail(userDetails.getUsername());
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Doctor doctor = getCurrentDoctor(userDetails);
        Map<String, Object> stats = appointmentService.getDoctorDashboardStats(doctor.getId());
        List<Appointment> todayAppointments = appointmentService.getDoctorAppointments(doctor.getId(), "TODAY");
        List<Appointment> pendingAppointments = appointmentService.getDoctorAppointments(doctor.getId(), "PENDING");

        model.addAttribute("doctor", doctor);
        model.addAttribute("stats", stats);
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("pendingAppointments", pendingAppointments);
        return "doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String appointments(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "filter", required = false, defaultValue = "ALL") String filter,
            Model model) {

        Doctor doctor = getCurrentDoctor(userDetails);
        List<Appointment> appointments = appointmentService.getDoctorAppointments(doctor.getId(), filter);

        model.addAttribute("doctor", doctor);
        model.addAttribute("appointments", appointments);
        model.addAttribute("currentFilter", filter.toUpperCase());
        return "doctor/appointments";
    }

    @PostMapping("/appointment/{id}/approve")
    public String approveAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        try {
            appointmentService.approveAppointment(id, doctor.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " has been approved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error approving appointment: " + e.getMessage());
        }

        return "redirect:/doctor/appointments";
    }

    @PostMapping("/appointment/{id}/reject")
    public String rejectAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "rejectionReason", required = false) String reason,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        try {
            appointmentService.rejectAppointment(id, doctor.getId(), reason);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " has been rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error rejecting appointment: " + e.getMessage());
        }

        return "redirect:/doctor/appointments";
    }

    @PostMapping("/appointment/{id}/complete")
    public String completeAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            @RequestParam(name = "doctorRemarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        try {
            appointmentService.completeAppointment(id, doctor.getId(), remarks);
            redirectAttributes.addFlashAttribute("successMessage", "Appointment #" + id + " has been marked as completed!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error completing appointment: " + e.getMessage());
        }

        return "redirect:/doctor/appointments";
    }

    @GetMapping("/availability")
    public String availability(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Doctor doctor = getCurrentDoctor(userDetails);
        List<Availability> availabilities = availabilityService.getDoctorAvailabilities(doctor.getId());

        AvailabilityDto availabilityDto = new AvailabilityDto();
        availabilityDto.setDayOfWeek(DayOfWeek.MONDAY);
        availabilityDto.setStartTime(LocalTime.of(9, 0));
        availabilityDto.setEndTime(LocalTime.of(13, 0));
        availabilityDto.setSlotDurationMinutes(30);

        model.addAttribute("doctor", doctor);
        model.addAttribute("availabilities", availabilities);
        model.addAttribute("availabilityDto", availabilityDto);
        model.addAttribute("daysOfWeek", DayOfWeek.values());
        return "doctor/availability";
    }

    @PostMapping("/availability/add")
    public String addAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("availabilityDto") AvailabilityDto dto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("availabilities", availabilityService.getDoctorAvailabilities(doctor.getId()));
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            return "doctor/availability";
        }

        try {
            availabilityService.addAvailability(doctor, dto);
            redirectAttributes.addFlashAttribute("successMessage", "Availability slot added successfully!");
            return "redirect:/doctor/availability";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("doctor", doctor);
            model.addAttribute("availabilities", availabilityService.getDoctorAvailabilities(doctor.getId()));
            model.addAttribute("daysOfWeek", DayOfWeek.values());
            return "doctor/availability";
        }
    }

    @PostMapping("/availability/{id}/delete")
    public String deleteAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("id") Long id,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        try {
            availabilityService.deleteAvailability(id, doctor.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Availability schedule removed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete availability: " + e.getMessage());
        }

        return "redirect:/doctor/availability";
    }

    @GetMapping("/patients")
    public String patients(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Doctor doctor = getCurrentDoctor(userDetails);
        List<Patient> patients = appointmentService.getPatientsTreatedByDoctor(doctor.getId());

        model.addAttribute("doctor", doctor);
        model.addAttribute("patients", patients);
        return "doctor/patients";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Doctor doctor = getCurrentDoctor(userDetails);
        DoctorProfileDto profileDto = doctorService.getProfileDto(doctor);

        model.addAttribute("doctor", doctor);
        model.addAttribute("profileDto", profileDto);
        model.addAttribute("changePasswordDto", new ChangePasswordDto());
        return "doctor/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("profileDto") DoctorProfileDto profileDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            return "doctor/profile";
        }

        try {
            doctorService.updateDoctorProfile(userDetails.getUsername(), profileDto);
            redirectAttributes.addFlashAttribute("successMessage", "Doctor profile updated successfully!");
            return "redirect:/doctor/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("doctor", doctor);
            model.addAttribute("changePasswordDto", new ChangePasswordDto());
            return "doctor/profile";
        }
    }

    @PostMapping("/change-password")
    public String changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @ModelAttribute("changePasswordDto") ChangePasswordDto changePasswordDto,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        Doctor doctor = getCurrentDoctor(userDetails);

        if (bindingResult.hasErrors()) {
            model.addAttribute("doctor", doctor);
            model.addAttribute("profileDto", doctorService.getProfileDto(doctor));
            return "doctor/profile";
        }

        try {
            authService.changePassword(userDetails.getUsername(), changePasswordDto);
            redirectAttributes.addFlashAttribute("successMessage", "Password updated successfully!");
            return "redirect:/doctor/profile";
        } catch (Exception e) {
            model.addAttribute("passwordErrorMessage", e.getMessage());
            model.addAttribute("doctor", doctor);
            model.addAttribute("profileDto", doctorService.getProfileDto(doctor));
            return "doctor/profile";
        }
    }
}
