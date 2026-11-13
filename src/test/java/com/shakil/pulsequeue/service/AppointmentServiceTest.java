package com.shakil.pulsequeue.service;

import com.shakil.pulsequeue.dto.AppointmentResponse;
import com.shakil.pulsequeue.dto.BookingRequest;
import com.shakil.pulsequeue.exception.SlotAlreadyBookedException;
import com.shakil.pulsequeue.model.*;
import com.shakil.pulsequeue.repository.AppointmentRepository;
import com.shakil.pulsequeue.repository.TimeSlotRepository;
import com.shakil.pulsequeue.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QueueService queueService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User testCustomer;
    private Department testDepartment;
    private TimeSlot testSlot;

    @BeforeEach
    void setUp() {
        testCustomer = new User("customer@pulsequeue.dev", "hashedPassword", "John Doe", "+880 1700-111111", Role.ROLE_CUSTOMER);
        testCustomer.setId(1L);

        testDepartment = new Department("General Medicine", "GEN", "General medical care");
        testDepartment.setId(1L);

        testSlot = new TimeSlot(testDepartment, LocalDate.now().plusDays(1), LocalTime.of(10, 0), LocalTime.of(10, 30));
        testSlot.setId(10L);
        testSlot.setVersion(1L);
    }

    @Test
    @DisplayName("Should successfully book available appointment slot")
    void testBookAppointment_Success() {
        BookingRequest request = new BookingRequest(10L, "Routine health checkup");

        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(Optional.of(testCustomer));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(testSlot));
        when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(testSlot);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment a = invocation.getArgument(0);
            a.setId(100L);
            return a;
        });

        AppointmentResponse response = appointmentService.bookAppointment(testCustomer.getEmail(), request);

        assertNotNull(response);
        assertEquals("John Doe", response.getCustomerName());
        assertEquals("General Medicine", response.getDepartmentName());
        assertTrue(testSlot.isBooked());

        verify(timeSlotRepository, times(1)).save(testSlot);
        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should reject booking if slot is already marked as booked")
    void testBookAppointment_SlotAlreadyBooked_ThrowsException() {
        testSlot.setBooked(true); // already booked
        BookingRequest request = new BookingRequest(10L, "Dental checkup");

        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(Optional.of(testCustomer));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(testSlot));

        assertThrows(SlotAlreadyBookedException.class, () -> {
            appointmentService.bookAppointment(testCustomer.getEmail(), request);
        });

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should handle JPA OptimisticLockingFailureException gracefully upon concurrency collision")
    void testBookAppointment_OptimisticLockingCollision() {
        BookingRequest request = new BookingRequest(10L, "Cardiac assessment");

        when(userRepository.findByEmail(testCustomer.getEmail())).thenReturn(Optional.of(testCustomer));
        when(timeSlotRepository.findById(10L)).thenReturn(Optional.of(testSlot));
        when(timeSlotRepository.save(any(TimeSlot.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(TimeSlot.class, 10L));

        SlotAlreadyBookedException exception = assertThrows(SlotAlreadyBookedException.class, () -> {
            appointmentService.bookAppointment(testCustomer.getEmail(), request);
        });

        assertTrue(exception.getMessage().contains("Concurrency collision"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
