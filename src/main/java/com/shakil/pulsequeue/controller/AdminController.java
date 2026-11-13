package com.shakil.pulsequeue.controller;

import com.shakil.pulsequeue.exception.ResourceNotFoundException;
import com.shakil.pulsequeue.model.Appointment;
import com.shakil.pulsequeue.model.Department;
import com.shakil.pulsequeue.model.QueueTicket;
import com.shakil.pulsequeue.model.ServiceCounter;
import com.shakil.pulsequeue.model.TimeSlot;
import com.shakil.pulsequeue.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin Operations", description = "CRUD management for departments, counters, slots, appointments, and system health")
public class AdminController {

    private final DepartmentRepository departmentRepository;
    private final ServiceCounterRepository counterRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final QueueTicketRepository queueTicketRepository;
    private final UserRepository userRepository;

    public AdminController(DepartmentRepository departmentRepository,
                           ServiceCounterRepository counterRepository,
                           TimeSlotRepository timeSlotRepository,
                           AppointmentRepository appointmentRepository,
                           QueueTicketRepository queueTicketRepository,
                           UserRepository userRepository) {
        this.departmentRepository = departmentRepository;
        this.counterRepository = counterRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.appointmentRepository = appointmentRepository;
        this.queueTicketRepository = queueTicketRepository;
        this.userRepository = userRepository;
    }

    // --- SYSTEM & DATABASE STATS ---
    @GetMapping("/stats")
    @Operation(summary = "Get live database counts and system metrics")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("databaseType", "H2 In-Memory (Active) / PostgreSQL 16 (Configured)");
        stats.put("departmentsCount", departmentRepository.count());
        stats.put("countersCount", counterRepository.count());
        stats.put("slotsCount", timeSlotRepository.count());
        stats.put("appointmentsCount", appointmentRepository.count());
        stats.put("ticketsCount", queueTicketRepository.count());
        stats.put("usersCount", userRepository.count());
        stats.put("h2ConsoleUrl", "http://localhost:8080/h2-console");
        return ResponseEntity.ok(stats);
    }

    // --- DEPARTMENT CRUD ---
    @GetMapping("/departments")
    @Operation(summary = "List all departments")
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping("/departments")
    @Operation(summary = "Create a new medical department")
    public ResponseEntity<Department> createDepartment(@Valid @RequestBody Department department) {
        return new ResponseEntity<>(departmentRepository.save(department), HttpStatus.CREATED);
    }

    @PutMapping("/departments/{id}")
    @Operation(summary = "Update department details")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @Valid @RequestBody Department updated) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id));
        dept.setName(updated.getName());
        dept.setCode(updated.getCode());
        dept.setDescription(updated.getDescription());
        return ResponseEntity.ok(departmentRepository.save(dept));
    }

    @DeleteMapping("/departments/{id}")
    @Operation(summary = "Delete department")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found: " + id);
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- COUNTER CRUD ---
    @GetMapping("/counters")
    @Operation(summary = "List all service counters (active and inactive)")
    public ResponseEntity<List<ServiceCounter>> getAllCounters() {
        return ResponseEntity.ok(counterRepository.findAll());
    }

    @PostMapping("/counters")
    @Operation(summary = "Create a new service counter")
    public ResponseEntity<ServiceCounter> createCounter(@Valid @RequestBody ServiceCounter counter) {
        return new ResponseEntity<>(counterRepository.save(counter), HttpStatus.CREATED);
    }

    @PutMapping("/counters/{id}/toggle")
    @Operation(summary = "Toggle counter active status")
    public ResponseEntity<ServiceCounter> toggleCounter(@PathVariable Long id) {
        ServiceCounter counter = counterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found: " + id));
        counter.setActive(!counter.isActive());
        return ResponseEntity.ok(counterRepository.save(counter));
    }

    @DeleteMapping("/counters/{id}")
    @Operation(summary = "Delete service counter")
    public ResponseEntity<Void> deleteCounter(@PathVariable Long id) {
        counterRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- TIME SLOTS CRUD ---
    @GetMapping("/slots")
    @Operation(summary = "List all time slots across departments")
    public ResponseEntity<List<TimeSlot>> getAllSlots() {
        return ResponseEntity.ok(timeSlotRepository.findAll());
    }

    @PostMapping("/slots")
    @Operation(summary = "Generate a new time slot")
    public ResponseEntity<TimeSlot> createSlot(@Valid @RequestBody TimeSlot slot) {
        return new ResponseEntity<>(timeSlotRepository.save(slot), HttpStatus.CREATED);
    }

    @DeleteMapping("/slots/{id}")
    @Operation(summary = "Delete an unbooked time slot")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        timeSlotRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- APPOINTMENTS AUDIT & MANAGEMENT ---
    @GetMapping("/appointments")
    @Operation(summary = "List all appointments across all patients for audit")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        return ResponseEntity.ok(appointmentRepository.findAll());
    }

    @DeleteMapping("/appointments/{id}")
    @Operation(summary = "Cancel / remove an appointment")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        Appointment apt = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        // Free up slot
        if (apt.getSlot() != null) {
            TimeSlot slot = apt.getSlot();
            slot.setBooked(false);
            timeSlotRepository.save(slot);
        }
        appointmentRepository.delete(apt);
        return ResponseEntity.noContent().build();
    }

    // --- TICKETS AUDIT ---
    @GetMapping("/tickets")
    @Operation(summary = "List all queue tickets across all counters")
    public ResponseEntity<List<QueueTicket>> getAllTickets() {
        return ResponseEntity.ok(queueTicketRepository.findAll());
    }
}
