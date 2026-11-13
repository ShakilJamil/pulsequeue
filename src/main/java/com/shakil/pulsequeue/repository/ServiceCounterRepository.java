package com.shakil.pulsequeue.repository;

import com.shakil.pulsequeue.model.ServiceCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCounterRepository extends JpaRepository<ServiceCounter, Long> {
    Optional<ServiceCounter> findByCounterNumber(String counterNumber);
    List<ServiceCounter> findByDepartmentId(Long departmentId);
    List<ServiceCounter> findByActiveTrue();
}
