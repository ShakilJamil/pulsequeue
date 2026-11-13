package com.shakil.pulsequeue.repository;

import com.shakil.pulsequeue.model.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDepartmentIdAndSlotDateAndBookedFalse(Long departmentId, LocalDate slotDate);
    List<TimeSlot> findBySlotDateAndBookedFalse(LocalDate slotDate);
    List<TimeSlot> findByDepartmentId(Long departmentId);
}
