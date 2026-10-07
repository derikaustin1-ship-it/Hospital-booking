package com.hospital.appointment.config;

import com.hospital.appointment.entity.*;
import com.hospital.appointment.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DoctorRepository doctorRepository,
                           PatientRepository patientRepository,
                           AvailabilityRepository availabilityRepository,
                           AppointmentRepository appointmentRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        System.out.println(">>> Initializing sample data for Hospital Doctor Appointment System...");

        // 1. Create Patient Users
        User patientUser1 = new User("John Doe", "patient@example.com", passwordEncoder.encode("Patient@123"), "+1 555-0199", Role.ROLE_PATIENT);
        User patientUser2 = new User("Jane Smith", "jane@example.com", passwordEncoder.encode("Patient@123"), "+1 555-0188", Role.ROLE_PATIENT);
        User patientUser3 = new User("Robert Johnson", "robert@example.com", passwordEncoder.encode("Patient@123"), "+1 555-0177", Role.ROLE_PATIENT);

        userRepository.saveAll(Arrays.asList(patientUser1, patientUser2, patientUser3));

        Patient patient1 = new Patient(patientUser1, LocalDate.of(1995, 5, 15), Gender.MALE, "452 Maple Street, Metro City");
        patient1.setBloodGroup("O+");
        patient1.setEmergencyContact("+1 555-0100");
        patient1.setMedicalHistory("Mild seasonal allergies. No major past surgeries.");

        Patient patient2 = new Patient(patientUser2, LocalDate.of(1992, 8, 22), Gender.FEMALE, "789 Pine Ridge Avenue, Metro City");
        patient2.setBloodGroup("A+");
        patient2.setEmergencyContact("+1 555-0101");

        Patient patient3 = new Patient(patientUser3, LocalDate.of(1988, 11, 30), Gender.MALE, "120 Oak Lane, Metro City");
        patient3.setBloodGroup("B+");
        patient3.setEmergencyContact("+1 555-0102");

        patientRepository.saveAll(Arrays.asList(patient1, patient2, patient3));

        // 2. Create Doctors
        // Doctor 1: Dr. Arun Kumar (Cardiologist) - Primary demo doctor
        User docUser1 = new User("Dr. Arun Kumar", "doctor@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0211", Role.ROLE_DOCTOR);
        userRepository.save(docUser1);

        Doctor doc1 = new Doctor(docUser1, "Cardiologist", "MD, DM Cardiology (AIIMS)", 12, new BigDecimal("800.00"),
                "Dr. Arun Kumar is a Senior Interventional Cardiologist with over 12 years of clinical experience specializing in preventive cardiology, coronary angioplasty, hypertension management, and heart failure care.");
        doc1.setProfileImage("https://images.unsplash.com/photo-1622253692010-333f2da6031d?w=300&h=300&fit=crop&crop=faces");
        doc1.setHospitalName("MediCare Multi-Speciality Hospital - Heart Institute");
        doc1.setClinicAddress("Suite 302, Cardiac Wing, 3rd Floor");
        doctorRepository.save(doc1);

        // Doctor 2: Dr. Priya Sharma (Dermatologist)
        User docUser2 = new User("Dr. Priya Sharma", "priya@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0222", Role.ROLE_DOCTOR);
        userRepository.save(docUser2);

        Doctor doc2 = new Doctor(docUser2, "Dermatologist", "MBBS, MD Dermatology", 8, new BigDecimal("600.00"),
                "Dr. Priya Sharma specializes in clinical dermatology, acne therapy, eczema treatment, cosmetic skin rejuvenation, and laser procedures with 8+ years of dedicated experience.");
        doc2.setProfileImage("https://images.unsplash.com/photo-1594824813590-78925d2b70f0?w=300&h=300&fit=crop&crop=faces");
        doc2.setHospitalName("MediCare Skin & Aesthetic Clinic");
        doc2.setClinicAddress("Suite 104, Skin Care Center, 1st Floor");
        doctorRepository.save(doc2);

        // Doctor 3: Dr. Rahul Menon (General Physician)
        User docUser3 = new User("Dr. Rahul Menon", "rahul@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0233", Role.ROLE_DOCTOR);
        userRepository.save(docUser3);

        Doctor doc3 = new Doctor(docUser3, "General Physician", "MBBS, MD Internal Medicine", 15, new BigDecimal("500.00"),
                "Dr. Rahul Menon is an experienced general physician focusing on adult disease diagnosis, diabetes control, lifestyle medicine, fever management, and comprehensive preventive health checkups.");
        doc3.setProfileImage("https://images.unsplash.com/photo-1537368910025-700350fe46c7?w=300&h=300&fit=crop&crop=faces");
        doc3.setHospitalName("MediCare Outpatient Department");
        doc3.setClinicAddress("OPD Room 12, Ground Floor");
        doctorRepository.save(doc3);

        // Doctor 4: Dr. Sneha Rao (Pediatrician)
        User docUser4 = new User("Dr. Sneha Rao", "sneha@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0244", Role.ROLE_DOCTOR);
        userRepository.save(docUser4);

        Doctor doc4 = new Doctor(docUser4, "Pediatrician", "MBBS, MD Pediatrics, DNB", 10, new BigDecimal("700.00"),
                "Dr. Sneha Rao is a compassionate pediatrician with 10 years of expertise in newborn care, childhood vaccinations, growth & developmental milestones, and pediatric respiratory conditions.");
        doc4.setProfileImage("https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=300&h=300&fit=crop&crop=faces");
        doc4.setHospitalName("MediCare Mother & Child Center");
        doc4.setClinicAddress("Pediatric Wing, 2nd Floor");
        doctorRepository.save(doc4);

        // Doctor 5: Dr. Vikram Patel (Orthopedic)
        User docUser5 = new User("Dr. Vikram Patel", "vikram@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0255", Role.ROLE_DOCTOR);
        userRepository.save(docUser5);

        Doctor doc5 = new Doctor(docUser5, "Orthopedic", "MBBS, MS Orthopedics, MCh", 14, new BigDecimal("900.00"),
                "Dr. Vikram Patel has 14 years of surgical experience in joint replacement, sports injury rehabilitation, fracture care, and spine disorder management.");
        doc5.setProfileImage("https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=300&h=300&fit=crop&crop=faces");
        doc5.setHospitalName("MediCare Bone & Joint Hospital");
        doc5.setClinicAddress("Orthopedic Suite 205, 2nd Floor");
        doctorRepository.save(doc5);

        // Doctor 6: Dr. Ananya Sen (Neurologist)
        User docUser6 = new User("Dr. Ananya Sen", "ananya@example.com", passwordEncoder.encode("Doctor@123"), "+1 555-0266", Role.ROLE_DOCTOR);
        userRepository.save(docUser6);

        Doctor doc6 = new Doctor(docUser6, "Neurologist", "MBBS, MD, DM Neurology", 11, new BigDecimal("1000.00"),
                "Dr. Ananya Sen specializes in migraine diagnosis, epilepsy, stroke management, Parkinson's disease, and neuromuscular disorders.");
        doc6.setProfileImage("https://images.unsplash.com/photo-1594824813683-2079f8c6d482?w=300&h=300&fit=crop&crop=faces");
        doc6.setHospitalName("MediCare Neurosciences Institute");
        doc6.setClinicAddress("Neuro OPD 401, 4th Floor");
        doctorRepository.save(doc6);

        // 3. Create Weekly Availabilities for Doctors
        List<Doctor> allDocs = Arrays.asList(doc1, doc2, doc3, doc4, doc5, doc6);
        DayOfWeek[] weekdays = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

        for (Doctor d : allDocs) {
            for (DayOfWeek day : weekdays) {
                // Morning session: 09:00 - 13:00 (30 min slots)
                Availability morning = new Availability(d, day, LocalTime.of(9, 0), LocalTime.of(13, 0), 30);
                // Evening session: 15:00 - 18:00 (30 min slots)
                Availability evening = new Availability(d, day, LocalTime.of(15, 0), LocalTime.of(18, 0), 30);
                availabilityRepository.save(morning);
                availabilityRepository.save(evening);
            }
        }

        // 4. Create Sample Appointments to showcase all states
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(3);
        LocalDate pastDate = today.minusDays(5);

        // Doctor 1 (Dr. Arun Kumar) Appointments
        // Today's appointment - Pending
        Appointment apt1 = new Appointment(patient1, doc1, today, LocalTime.of(10, 0), "Regular blood pressure checkup and chest tightness evaluation", "Patient reports mild fatigue.");
        apt1.setStatus(AppointmentStatus.APPROVED);

        // Today's appointment - Pending
        Appointment apt2 = new Appointment(patient2, doc1, today, LocalTime.of(11, 30), "Follow-up consultation after ECG test", "Previous report was normal.");
        apt2.setStatus(AppointmentStatus.PENDING);

        // Tomorrow's appointment - Approved
        Appointment apt3 = new Appointment(patient1, doc1, tomorrow, LocalTime.of(9, 30), "Cholesterol review & dietary consultation", "Taking Atorvastatin 10mg.");
        apt3.setStatus(AppointmentStatus.APPROVED);

        // Next week appointment - Pending
        Appointment apt4 = new Appointment(patient3, doc1, nextWeek, LocalTime.of(15, 0), "Cardiology second opinion", "Referred by family physician.");
        apt4.setStatus(AppointmentStatus.PENDING);

        // Past completed appointment
        Appointment apt5 = new Appointment(patient1, doc1, pastDate, LocalTime.of(10, 0), "Routine cardiac stress test follow-up", "Completed successfully without complications.");
        apt5.setStatus(AppointmentStatus.COMPLETED);
        apt5.setDoctorRemarks("ECG normal. Advised 30 minutes daily walking and low-sodium diet. Review in 3 months.");

        // Cancelled appointment
        Appointment apt6 = new Appointment(patient2, doc1, pastDate.minusDays(2), LocalTime.of(16, 0), "General health review", "Cancelled due to personal travel.");
        apt6.setStatus(AppointmentStatus.CANCELLED);
        apt6.setCancellationReason("Patient requested cancellation due to travel schedule.");

        // Doctor 2 (Dr. Priya Sharma) Appointments
        Appointment apt7 = new Appointment(patient1, doc2, tomorrow, LocalTime.of(10, 0), "Skin allergy and rash checkup", "Appeared after new detergent.");
        apt7.setStatus(AppointmentStatus.APPROVED);

        // Doctor 3 (Dr. Rahul Menon) Appointments
        Appointment apt8 = new Appointment(patient1, doc3, pastDate.minusDays(10), LocalTime.of(11, 0), "Annual health physical examination", "All vital metrics normal.");
        apt8.setStatus(AppointmentStatus.COMPLETED);
        apt8.setDoctorRemarks("Prescribed Multivitamins. Health parameters within standard range.");

        appointmentRepository.saveAll(Arrays.asList(apt1, apt2, apt3, apt4, apt5, apt6, apt7, apt8));

        System.out.println(">>> Sample data initialization completed successfully!");
        System.out.println(">>> Demo Doctor Login: doctor@example.com / Doctor@123");
        System.out.println(">>> Demo Patient Login: patient@example.com / Patient@123");
    }
}
