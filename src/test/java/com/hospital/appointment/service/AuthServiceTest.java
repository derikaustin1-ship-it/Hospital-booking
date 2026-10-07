package com.hospital.appointment.service;

import com.hospital.appointment.dto.ChangePasswordDto;
import com.hospital.appointment.dto.RegistrationRequest;
import com.hospital.appointment.entity.Gender;
import com.hospital.appointment.entity.Patient;
import com.hospital.appointment.entity.Role;
import com.hospital.appointment.entity.User;
import com.hospital.appointment.repository.PatientRepository;
import com.hospital.appointment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegistrationRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegistrationRequest();
        validRequest.setName("Alice Wonderland");
        validRequest.setEmail("alice@example.com");
        validRequest.setPhone("+1 555-9999");
        validRequest.setDateOfBirth(LocalDate.of(1996, 4, 10));
        validRequest.setGender(Gender.FEMALE);
        validRequest.setBloodGroup("B+");
        validRequest.setAddress("123 Health Lane");
        validRequest.setPassword("Secret@123");
        validRequest.setConfirmPassword("Secret@123");
    }

    @Test
    void testRegisterPatient_Success() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("encodedPassword");

        User savedUser = new User("Alice Wonderland", "alice@example.com", "encodedPassword", "+1 555-9999", Role.ROLE_PATIENT);
        savedUser.setId(10L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        Patient savedPatient = new Patient(savedUser, LocalDate.of(1996, 4, 10), Gender.FEMALE, "123 Health Lane");
        savedPatient.setId(20L);
        when(patientRepository.save(any(Patient.class))).thenReturn(savedPatient);

        Patient result = authService.registerPatient(validRequest);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        assertEquals("alice@example.com", result.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    void testRegisterPatient_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerPatient(validRequest);
        });

        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterPatient_PasswordMismatch_ThrowsException() {
        validRequest.setConfirmPassword("DifferentPassword");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.registerPatient(validRequest);
        });

        assertTrue(exception.getMessage().contains("match"));
    }

    @Test
    void testChangePassword_Success() {
        User user = new User("Alice", "alice@example.com", "hashedOldPassword", "+1 555-9999", Role.ROLE_PATIENT);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123", "hashedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("hashedNewPassword");

        ChangePasswordDto dto = new ChangePasswordDto();
        dto.setCurrentPassword("OldPassword123");
        dto.setNewPassword("NewPassword123");
        dto.setConfirmPassword("NewPassword123");

        authService.changePassword("alice@example.com", dto);

        assertEquals("hashedNewPassword", user.getPassword());
        verify(userRepository, times(1)).save(user);
    }
}
