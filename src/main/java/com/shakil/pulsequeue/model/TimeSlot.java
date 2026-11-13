package com.shakil.pulsequeue.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents an appointment time slot.
 * Utilizes JPA @Version for Optimistic Locking to eliminate race conditions
 * when multiple clients attempt to book the identical slot concurrently.
 */
@Entity
@Table(name = "time_slots", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"department_id", "slotDate", "startTime"})
})
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean booked = false;

    /**
     * Optimistic locking version column.
     * Prevents race conditions during concurrent bookings.
     */
    @Version
    private Long version;

    public TimeSlot() {}

    public TimeSlot(Department department, LocalDate slotDate, LocalTime startTime, LocalTime endTime) {
        this.department = department;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.booked = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public boolean isBooked() { return booked; }
    public void setBooked(boolean booked) { this.booked = booked; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
