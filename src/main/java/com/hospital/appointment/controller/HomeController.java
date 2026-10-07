package com.hospital.appointment.controller;

import com.hospital.appointment.dto.DoctorSearchFilterDto;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.service.DoctorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {

    private final DoctorService doctorService;

    public HomeController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Doctor> featuredDoctors = doctorService.getAllActiveDoctors();
        if (featuredDoctors.size() > 4) {
            featuredDoctors = featuredDoctors.subList(0, 4);
        }
        model.addAttribute("featuredDoctors", featuredDoctors);
        model.addAttribute("specializations", doctorService.getAllSpecializations());
        return "index";
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

    @GetMapping("/about")
    public String about() {
        return "about";
    }

    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}
