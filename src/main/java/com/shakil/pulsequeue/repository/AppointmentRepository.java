package com.shakil.pulsequeue.repository;

import com.shakil.pulsequeue.model.Appointment;
import com.shakil.pulsequeue.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByAppointmentCode(String appointmentCode);
    List<Appointment> findByCustomerId(Long customerId);
    List<Appointment> findByStatus(AppointmentStatus status);
    List<Appointment> findByTimeSlotDepartmentId(Long departmentId);
}
