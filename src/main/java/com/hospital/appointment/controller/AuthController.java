package com.hospital.appointment.controller;

import com.hospital.appointment.dto.LoginRequest;
import com.hospital.appointment.dto.RegistrationRequest;
import com.hospital.appointment.entity.Gender;
import com.hospital.appointment.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "registered", required = false) String registered,
            Model model) {

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email address or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        if (registered != null) {
            model.addAttribute("successMessage", "Registration completed successfully! Please login with your credentials.");
        }

        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("genders", Gender.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerPatient(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        model.addAttribute("genders", Gender.values());

        // Validate password matching
        if (request.getPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match");
        }

        // Validate unique email
        if (request.getEmail() != null && authService.isEmailTaken(request.getEmail())) {
            bindingResult.rejectValue("email", "error.email", "This email is already registered");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.registerPatient(request);
            redirectAttributes.addFlashAttribute("successMessage", "Your patient account has been created successfully! Please log in.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
}
