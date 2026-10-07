package com.hospital.appointment.service;

import com.hospital.appointment.dto.AppointmentBookingRequest;
import com.hospital.appointment.dto.TimeSlotDto;
import com.hospital.appointment.entity.*;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.DoctorRepository;
import com.hospital.appointment.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AppointmentService appointmentService;

    private Patient samplePatient;
    private Doctor sampleDoctor;
    private User docUser;
    private User patUser;

    @BeforeEach
    void setUp() {
        patUser = new User("John Doe", "patient@example.com", "hash", "+1 555-0199", Role.ROLE_PATIENT);
        samplePatient = new Patient(patUser, LocalDate.of(1995, 5, 15), Gender.MALE, "123 Main St");
        samplePatient.setId(1L);

        docUser = new User("Dr. Arun Kumar", "doctor@example.com", "hash", "+1 555-0211", Role.ROLE_DOCTOR);
        sampleDoctor = new Doctor(docUser, "Cardiologist", "MD Cardiology", 12, new BigDecimal("800.00"), "Bio");
        sampleDoctor.setId(10L);
        sampleDoctor.setAvailable(true);
    }

    @Test
    void testBookAppointment_Success() {
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentBookingRequest req = new AppointmentBookingRequest(10L, date, time, "Chest pain", "Notes");

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(sampleDoctor));
        TimeSlotDto slot = new TimeSlotDto(time, time.plusMinutes(30), "10:00 AM - 10:30 AM", true);
        when(availabilityService.getAvailableTimeSlots(10L, date)).thenReturn(List.of(slot));
        when(appointmentRepository.existsActiveAppointmentAtDateTime(10L, date, time)).thenReturn(false);

        Appointment savedApt = new Appointment(samplePatient, sampleDoctor, date, time, "Chest pain", "Notes");
        savedApt.setId(100L);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedApt);

        Appointment result = appointmentService.bookAppointment(samplePatient, req);

        assertNotNull(result);
        assertEquals(AppointmentStatus.PENDING, result.getStatus());
        assertEquals(100L, result.getId());
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void testBookAppointment_DoubleBooking_ThrowsException() {
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(10, 0);

        AppointmentBookingRequest req = new AppointmentBookingRequest(10L, date, time, "Chest pain", "Notes");

        when(doctorRepository.findById(10L)).thenReturn(Optional.of(sampleDoctor));
        TimeSlotDto slot = new TimeSlotDto(time, time.plusMinutes(30), "10:00 AM - 10:30 AM", true);
        when(availabilityService.getAvailableTimeSlots(10L, date)).thenReturn(List.of(slot));
        when(appointmentRepository.existsActiveAppointmentAtDateTime(10L, date, time)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.bookAppointment(samplePatient, req);
        });

        assertTrue(exception.getMessage().contains("booked by another patient"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void testApproveAppointment_Success() {
        Appointment apt = new Appointment(samplePatient, sampleDoctor, LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Reason", "Notes");
        apt.setId(100L);
        apt.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(apt);

        Appointment approved = appointmentService.approveAppointment(100L, 10L);

        assertEquals(AppointmentStatus.APPROVED, approved.getStatus());
        verify(appointmentRepository, times(1)).save(apt);
    }

    @Test
    void testApproveAppointment_UnauthorizedDoctor_ThrowsException() {
        Appointment apt = new Appointment(samplePatient, sampleDoctor, LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Reason", "Notes");
        apt.setId(100L);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(apt));

        assertThrows(SecurityException.class, () -> {
            appointmentService.approveAppointment(100L, 999L); // Wrong doctor ID
        });
    }

    @Test
    void testCompleteAppointment_Success() {
        Appointment apt = new Appointment(samplePatient, sampleDoctor, LocalDate.now(), LocalTime.of(10, 0), "Reason", "Notes");
        apt.setId(100L);
        apt.setStatus(AppointmentStatus.APPROVED);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(apt);

        Appointment completed = appointmentService.completeAppointment(100L, 10L, "Prescribed Aspirin 75mg");

        assertEquals(AppointmentStatus.COMPLETED, completed.getStatus());
        assertEquals("Prescribed Aspirin 75mg", completed.getDoctorRemarks());
    }

    @Test
    void testCancelAppointment_Future_Success() {
        Appointment apt = new Appointment(samplePatient, sampleDoctor, LocalDate.now().plusDays(3), LocalTime.of(10, 0), "Reason", "Notes");
        apt.setId(100L);
        apt.setStatus(AppointmentStatus.PENDING);

        when(appointmentRepository.findById(100L)).thenReturn(Optional.of(apt));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(apt);

        Appointment cancelled = appointmentService.cancelAppointmentByPatient(100L, 1L, "Schedule conflict");

        assertEquals(AppointmentStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Schedule conflict", cancelled.getCancellationReason());
    }
}
