package com.hospital.appointment.service;

import com.hospital.appointment.dto.AvailabilityDto;
import com.hospital.appointment.dto.TimeSlotDto;
import com.hospital.appointment.entity.Appointment;
import com.hospital.appointment.entity.Availability;
import com.hospital.appointment.entity.Doctor;
import com.hospital.appointment.repository.AppointmentRepository;
import com.hospital.appointment.repository.AvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    public AvailabilityService(AvailabilityRepository availabilityRepository,
                               AppointmentRepository appointmentRepository) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public List<Availability> getDoctorAvailabilities(Long doctorId) {
        return availabilityRepository.findByDoctorId(doctorId);
    }

    public List<Availability> getDoctorAvailabilitiesByDay(Long doctorId, DayOfWeek dayOfWeek) {
        return availabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
    }

    @Transactional
    public Availability addAvailability(Doctor doctor, AvailabilityDto dto) {
        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
            throw new IllegalArgumentException("Start time must be strictly before end time.");
        }

        // Check for overlapping intervals
        List<Availability> overlapping = availabilityRepository.findOverlappingAvailabilities(
                doctor.getId(),
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("This availability schedule overlaps with an existing schedule for " + dto.getDayOfWeek());
        }

        Availability availability = new Availability(
                doctor,
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getSlotDurationMinutes() != null ? dto.getSlotDurationMinutes() : 30
        );

        return availabilityRepository.save(availability);
    }

    @Transactional
    public void deleteAvailability(Long availabilityId, Long doctorId) {
        Availability availability = availabilityRepository.findById(availabilityId)
                .orElseThrow(() -> new IllegalArgumentException("Availability slot not found with id: " + availabilityId));

        if (!availability.getDoctor().getId().equals(doctorId)) {
            throw new SecurityException("Unauthorized: You do not have permission to delete this availability.");
        }

        availabilityRepository.delete(availability);
    }

    /**
     * Generates all appointment slots for a given doctor on a given date,
     * checking against both doctor availability and existing booked appointments.
     */
    public List<TimeSlotDto> getAvailableTimeSlots(Long doctorId, LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            return Collections.emptyList();
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<Availability> availabilities = availabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);

        if (availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        // Get active appointments on that day for that doctor
        List<Appointment> bookedAppointments = appointmentRepository.findActiveAppointmentsByDoctorAndDate(doctorId, date);
        Set<LocalTime> bookedTimes = bookedAppointments.stream()
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        List<TimeSlotDto> slotList = new ArrayList<>();
        LocalTime nowTime = LocalTime.now();
        boolean isToday = date.equals(LocalDate.now());

        for (Availability availability : availabilities) {
            LocalTime current = availability.getStartTime();
            int duration = availability.getSlotDurationMinutes() > 0 ? availability.getSlotDurationMinutes() : 30;

            while (current.plusMinutes(duration).isBefore(availability.getEndTime()) ||
                   current.plusMinutes(duration).equals(availability.getEndTime())) {
                
                LocalTime slotEnd = current.plusMinutes(duration);
                
                // If date is today, slot must be in the future
                boolean isInPastToday = isToday && current.isBefore(nowTime);
                boolean isBooked = bookedTimes.contains(current);
                boolean isAvailable = !isInPastToday && !isBooked;

                String formatted = current.format(TIME_FORMATTER) + " - " + slotEnd.format(TIME_FORMATTER);
                slotList.add(new TimeSlotDto(current, slotEnd, formatted, isAvailable));

                current = current.plusMinutes(duration);
            }
        }

        // Sort chronologically
        slotList.sort(Comparator.comparing(TimeSlotDto::getStartTime));
        return slotList;
    }
}
