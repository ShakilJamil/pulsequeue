package com.shakil.pulsequeue.controller;

import com.shakil.pulsequeue.dto.AppointmentResponse;
import com.shakil.pulsequeue.dto.BookingRequest;
import com.shakil.pulsequeue.model.Department;
import com.shakil.pulsequeue.model.TimeSlot;
import com.shakil.pulsequeue.repository.DepartmentRepository;
import com.shakil.pulsequeue.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Appointments & Slots", description = "Concurrency-safe appointment slot booking and availability")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final DepartmentRepository departmentRepository;

    public AppointmentController(AppointmentService appointmentService, DepartmentRepository departmentRepository) {
        this.appointmentService = appointmentService;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/departments")
    @Operation(summary = "List all active clinic departments")
    public ResponseEntity<List<Department>> getDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @GetMapping("/slots")
    @Operation(summary = "Query available time slots by department and date")
    public ResponseEntity<List<TimeSlot>> getAvailableSlots(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(appointmentService.getAvailableSlots(departmentId, date));
    }

    @PostMapping("/appointments/book")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reserve an appointment slot (Protected with JPA @Version Optimistic Locking)")
    public ResponseEntity<AppointmentResponse> bookAppointment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BookingRequest request) {
        return new ResponseEntity<>(
                appointmentService.bookAppointment(userDetails.getUsername(), request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve current authenticated user's scheduled appointments")
    public ResponseEntity<List<AppointmentResponse>> getMyAppointments(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(appointmentService.getCustomerAppointments(userDetails.getUsername()));
    }

    @GetMapping("/appointments/code/{code}")
    @Operation(summary = "Lookup appointment verification by unique tracking code")
    public ResponseEntity<AppointmentResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(appointmentService.getAppointmentByCode(code));
    }
}
