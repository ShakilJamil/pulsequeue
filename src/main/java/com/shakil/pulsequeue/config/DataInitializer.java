package com.shakil.pulsequeue.config;

import com.shakil.pulsequeue.model.*;
import com.shakil.pulsequeue.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ServiceCounterRepository counterRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DepartmentRepository departmentRepository,
                           ServiceCounterRepository counterRepository,
                           TimeSlotRepository timeSlotRepository,
                           QueueTicketRepository queueTicketRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.counterRepository = counterRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.queueTicketRepository = queueTicketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        // 1. Seed Users
        User admin = userRepository.save(new User(
                "admin@pulsequeue.dev",
                passwordEncoder.encode("Admin@123"),
                "Dr. Sarah Mitchell",
                "+880 1700-111222",
                Role.ROLE_ADMIN
        ));

        User staff1 = userRepository.save(new User(
                "staff1@pulsequeue.dev",
                passwordEncoder.encode("Staff@123"),
                "Dr. Shakil Jamil",
                "+880 1700-656279",
                Role.ROLE_STAFF
        ));

        User staff2 = userRepository.save(new User(
                "staff2@pulsequeue.dev",
                passwordEncoder.encode("Staff@123"),
                "Nurse Emily Watson",
                "+880 1700-333444",
                Role.ROLE_STAFF
        ));

        User customer1 = userRepository.save(new User(
                "customer@pulsequeue.dev",
                passwordEncoder.encode("Customer@123"),
                "Alex Rivera",
                "+880 1700-555666",
                Role.ROLE_CUSTOMER
        ));

        // 2. Seed Departments
        Department depGeneral = departmentRepository.save(new Department(
                "General Medicine",
                "GEN",
                "Primary outpatient care, vital examinations, and consultations."
        ));

        Department depCardio = departmentRepository.save(new Department(
                "Cardiology",
                "CARD",
                "Cardiovascular health, ECG assessments, and specialist evaluations."
        ));

        Department depLab = departmentRepository.save(new Department(
                "Pathology & Diagnostics Lab",
                "LAB",
                "Blood diagnostics, sample collections, and rapid screening reports."
        ));

        // 3. Seed Service Counters
        ServiceCounter counter1 = counterRepository.save(new ServiceCounter("Counter 101", depGeneral, staff1));
        ServiceCounter counter2 = counterRepository.save(new ServiceCounter("Counter 102", depCardio, staff2));
        ServiceCounter counter3 = counterRepository.save(new ServiceCounter("Counter 103", depLab, staff1));

        // 4. Seed Time Slots for Today and Tomorrow
        LocalDate today = LocalDate.now();
        LocalTime[] times = {
                LocalTime.of(9, 0), LocalTime.of(9, 30),
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalTime.of(11, 0), LocalTime.of(11, 30),
                LocalTime.of(14, 0), LocalTime.of(14, 30),
                LocalTime.of(15, 0), LocalTime.of(15, 30)
        };

        for (LocalTime t : times) {
            timeSlotRepository.save(new TimeSlot(depGeneral, today, t, t.plusMinutes(25)));
            timeSlotRepository.save(new TimeSlot(depCardio, today, t, t.plusMinutes(25)));
            timeSlotRepository.save(new TimeSlot(depLab, today, t, t.plusMinutes(25)));

            // Tomorrow
            timeSlotRepository.save(new TimeSlot(depGeneral, today.plusDays(1), t, t.plusMinutes(25)));
            timeSlotRepository.save(new TimeSlot(depCardio, today.plusDays(1), t, t.plusMinutes(25)));
        }

        // 5. Seed Initial Queue Tickets
        QueueTicket ticket1 = new QueueTicket("Q-101", null, depGeneral, 1);
        ticket1.setStatus(TicketStatus.SERVING);
        ticket1.setCounter(counter1);
        ticket1.setCalledAt(java.time.LocalDateTime.now().minusMinutes(5));
        queueTicketRepository.save(ticket1);

        QueueTicket ticket2 = new QueueTicket("Q-102", null, depGeneral, 2);
        ticket2.setStatus(TicketStatus.WAITING);
        queueTicketRepository.save(ticket2);

        QueueTicket ticket3 = new QueueTicket("Q-103", null, depGeneral, 3);
        ticket3.setStatus(TicketStatus.WAITING);
        queueTicketRepository.save(ticket3);

        QueueTicket ticket4 = new QueueTicket("Q-201", null, depCardio, 1);
        ticket4.setStatus(TicketStatus.SERVING);
        ticket4.setCounter(counter2);
        ticket4.setCalledAt(java.time.LocalDateTime.now().minusMinutes(2));
        queueTicketRepository.save(ticket4);

        QueueTicket ticket5 = new QueueTicket("Q-202", null, depCardio, 2);
        ticket5.setStatus(TicketStatus.WAITING);
        queueTicketRepository.save(ticket5);
    }
}
