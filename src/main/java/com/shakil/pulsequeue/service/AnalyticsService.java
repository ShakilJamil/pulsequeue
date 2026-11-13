package com.shakil.pulsequeue.service;

import com.shakil.pulsequeue.dto.QueueStatsResponse;
import com.shakil.pulsequeue.model.Department;
import com.shakil.pulsequeue.model.TicketStatus;
import com.shakil.pulsequeue.repository.DepartmentRepository;
import com.shakil.pulsequeue.repository.QueueTicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final QueueTicketRepository ticketRepository;
    private final DepartmentRepository departmentRepository;

    public AnalyticsService(QueueTicketRepository ticketRepository, DepartmentRepository departmentRepository) {
        this.ticketRepository = ticketRepository;
        this.departmentRepository = departmentRepository;
    }

    public QueueStatsResponse getLiveStats() {
        long waiting = ticketRepository.countByStatus(TicketStatus.WAITING);
        long serving = ticketRepository.countByStatus(TicketStatus.SERVING);
        long completedToday = ticketRepository.countCompletedSince(LocalDate.now().atStartOfDay());

        Map<String, Long> byDepartment = new HashMap<>();
        List<Department> departments = departmentRepository.findAll();
        for (Department d : departments) {
            byDepartment.put(d.getName(), ticketRepository.countByDepartmentIdAndStatus(d.getId(), TicketStatus.WAITING));
        }

        // Estimated average wait time calculation (heuristic: 8 mins per ticket ahead)
        double avgWaitMinutes = waiting > 0 ? waiting * 7.5 : 0.0;

        return new QueueStatsResponse(waiting, serving, completedToday, avgWaitMinutes, byDepartment);
    }
}
