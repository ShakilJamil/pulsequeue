package com.shakil.pulsequeue.dto;

import com.shakil.pulsequeue.model.Appointment;
import com.shakil.pulsequeue.model.AppointmentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AppointmentResponse {

    private Long id;
    private String appointmentCode;
    private String customerName;
    private String customerEmail;
    private String departmentName;
    private LocalDate slotDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private AppointmentStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public AppointmentResponse() {}

    public static AppointmentResponse fromEntity(Appointment apt) {
        AppointmentResponse res = new AppointmentResponse();
        res.setId(apt.getId());
        res.setAppointmentCode(apt.getAppointmentCode());
        res.setCustomerName(apt.getCustomer().getFullName());
        res.setCustomerEmail(apt.getCustomer().getEmail());
        res.setDepartmentName(apt.getTimeSlot().getDepartment().getName());
        res.setSlotDate(apt.getTimeSlot().getSlotDate());
        res.setStartTime(apt.getTimeSlot().getStartTime());
        res.setEndTime(apt.getTimeSlot().getEndTime());
        res.setStatus(apt.getStatus());
        res.setNotes(apt.getNotes());
        res.setCreatedAt(apt.getCreatedAt());
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAppointmentCode() { return appointmentCode; }
    public void setAppointmentCode(String appointmentCode) { this.appointmentCode = appointmentCode; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
