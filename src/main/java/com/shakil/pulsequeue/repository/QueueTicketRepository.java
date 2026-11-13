package com.shakil.pulsequeue.repository;

import com.shakil.pulsequeue.model.QueueTicket;
import com.shakil.pulsequeue.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueTicketRepository extends JpaRepository<QueueTicket, Long> {
    Optional<QueueTicket> findByTicketNumber(String ticketNumber);
    List<QueueTicket> findByStatusOrderBySequenceNumberAsc(TicketStatus status);
    List<QueueTicket> findByDepartmentIdAndStatusOrderBySequenceNumberAsc(Long departmentId, TicketStatus status);
    Optional<QueueTicket> findFirstByDepartmentIdAndStatusOrderBySequenceNumberAsc(Long departmentId, TicketStatus status);
    Optional<QueueTicket> findFirstByStatusOrderBySequenceNumberAsc(TicketStatus status);
    
    long countByStatus(TicketStatus status);
    long countByDepartmentIdAndStatus(Long departmentId, TicketStatus status);
    long countByDepartmentIdAndStatusAndSequenceNumberLessThan(Long departmentId, TicketStatus status, int sequenceNumber);

    @Query("SELECT COUNT(t) FROM QueueTicket t WHERE t.status = 'COMPLETED' AND t.completedAt >= :since")
    long countCompletedSince(LocalDateTime since);

    List<QueueTicket> findTop10ByOrderByIssuedAtDesc();
}
