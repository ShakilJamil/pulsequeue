package com.shakil.pulsequeue.service;

import com.shakil.pulsequeue.dto.AppointmentResponse;
import com.shakil.pulsequeue.dto.BookingRequest;
import com.shakil.pulsequeue.exception.ResourceNotFoundException;
import com.shakil.pulsequeue.exception.SlotAlreadyBookedException;
import com.shakil.pulsequeue.model.*;
import com.shakil.pulsequeue.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final UserRepository userRepository;
    private final QueueService queueService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              TimeSlotRepository timeSlotRepository,
                              UserRepository userRepository,
                              QueueService queueService) {
        this.appointmentRepository = appointmentRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.userRepository = userRepository;
        this.queueService = queueService;
    }

    /**
     * Books an appointment slot under transactional isolation.
     * Prevents race conditions using JPA @Version Optimistic Locking.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentResponse bookAppointment(String customerEmail, BookingRequest request) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerEmail));

        TimeSlot slot = timeSlotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot not found with ID: " + request.getSlotId()));

        if (slot.isBooked()) {
            throw new SlotAlreadyBookedException("Slot ID " + slot.getId() + " is already booked by another patient.");
        }

        try {
            // Mark as booked - Hibernate checks version column upon flush
            slot.setBooked(true);
            timeSlotRepository.save(slot);

            String appointmentCode = "APT-" + LocalDate.now().getYear() + "-"
                    + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            Appointment appointment = new Appointment(appointmentCode, customer, slot, request.getNotes());
            Appointment savedAppointment = appointmentRepository.save(appointment);

            // Automatically issue queue ticket for today's appointments
            if (slot.getSlotDate().equals(LocalDate.now())) {
                queueService.issueTicketForAppointment(savedAppointment);
            }

            log.info("Successfully booked appointment {} for customer {}", appointmentCode, customerEmail);
            return AppointmentResponse.fromEntity(savedAppointment);

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.warn("Optimistic locking conflict detected on Slot ID {}", slot.getId());
            throw new SlotAlreadyBookedException(
                "Concurrency collision: Slot ID " + slot.getId() + " was just claimed by another user. Please select another slot."
            );
        }
    }

    public List<AppointmentResponse> getCustomerAppointments(String customerEmail) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerEmail));

        return appointmentRepository.findByCustomerId(customer.getId())
                .stream()
                .map(AppointmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public List<TimeSlot> getAvailableSlots(Long departmentId, LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        if (departmentId != null) {
            return timeSlotRepository.findByDepartmentIdAndSlotDateAndBookedFalse(departmentId, queryDate);
        }
        return timeSlotRepository.findBySlotDateAndBookedFalse(queryDate);
    }

    public AppointmentResponse getAppointmentByCode(String code) {
        Appointment apt = appointmentRepository.findByAppointmentCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with code: " + code));
        return AppointmentResponse.fromEntity(apt);
    }
}
