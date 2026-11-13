package com.shakil.pulsequeue.service;

import com.shakil.pulsequeue.dto.QueueTicketResponse;
import com.shakil.pulsequeue.exception.InvalidQueueOperationException;
import com.shakil.pulsequeue.exception.ResourceNotFoundException;
import com.shakil.pulsequeue.model.*;
import com.shakil.pulsequeue.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class QueueService {

    private static final Logger log = LoggerFactory.getLogger(QueueService.class);

    private final QueueTicketRepository ticketRepository;
    private final ServiceCounterRepository counterRepository;
    private final DepartmentRepository departmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public QueueService(QueueTicketRepository ticketRepository,
                        ServiceCounterRepository counterRepository,
                        DepartmentRepository departmentRepository,
                        SimpMessagingTemplate messagingTemplate) {
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.departmentRepository = departmentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public QueueTicketResponse issueTicketForAppointment(Appointment appointment) {
        Department dep = appointment.getTimeSlot().getDepartment();
        int seq = (int) ticketRepository.countByDepartmentIdAndStatus(dep.getId(), TicketStatus.WAITING) + 1;
        String ticketNumber = dep.getCode() + "-" + (100 + seq);

        QueueTicket ticket = new QueueTicket(ticketNumber, appointment, dep, seq);
        QueueTicket saved = ticketRepository.save(ticket);

        broadcastQueueUpdate("TICKET_ISSUED", saved);
        return QueueTicketResponse.fromEntity(saved, calculateWaitingAhead(saved));
    }

    @Transactional
    public QueueTicketResponse issueWalkInTicket(Long departmentId) {
        Department dep = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));

        int seq = (int) ticketRepository.countByDepartmentIdAndStatus(dep.getId(), TicketStatus.WAITING) + 1;
        String ticketNumber = dep.getCode() + "-" + (100 + seq);

        QueueTicket ticket = new QueueTicket(ticketNumber, null, dep, seq);
        QueueTicket saved = ticketRepository.save(ticket);

        broadcastQueueUpdate("TICKET_ISSUED", saved);
        return QueueTicketResponse.fromEntity(saved, calculateWaitingAhead(saved));
    }

    @Transactional
    public QueueTicketResponse callNextTicket(Long counterId) {
        ServiceCounter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found: " + counterId));

        // Find earliest waiting ticket in counter's department
        QueueTicket nextTicket = ticketRepository
                .findFirstByDepartmentIdAndStatusOrderBySequenceNumberAsc(counter.getDepartment().getId(), TicketStatus.WAITING)
                .orElseThrow(() -> new InvalidQueueOperationException("No waiting tickets in department: " + counter.getDepartment().getName()));

        nextTicket.setStatus(TicketStatus.CALLED);
        nextTicket.setCounter(counter);
        nextTicket.setCalledAt(LocalDateTime.now());
        QueueTicket saved = ticketRepository.save(nextTicket);

        log.info("Counter {} called Ticket {}", counter.getCounterNumber(), saved.getTicketNumber());
        broadcastQueueUpdate("TICKET_CALLED", saved);
        return QueueTicketResponse.fromEntity(saved, 0);
    }

    @Transactional
    public QueueTicketResponse startServing(Long ticketId) {
        QueueTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.SERVING);
        QueueTicket saved = ticketRepository.save(ticket);

        broadcastQueueUpdate("TICKET_SERVING", saved);
        return QueueTicketResponse.fromEntity(saved, 0);
    }

    @Transactional
    public QueueTicketResponse completeService(Long ticketId) {
        QueueTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());
        QueueTicket saved = ticketRepository.save(ticket);

        broadcastQueueUpdate("TICKET_COMPLETED", saved);
        return QueueTicketResponse.fromEntity(saved, 0);
    }

    @Transactional
    public QueueTicketResponse markNoShow(Long ticketId) {
        QueueTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketId));

        ticket.setStatus(TicketStatus.SKIPPED);
        QueueTicket saved = ticketRepository.save(ticket);

        broadcastQueueUpdate("TICKET_SKIPPED", saved);
        return QueueTicketResponse.fromEntity(saved, 0);
    }

    public List<QueueTicketResponse> getActiveQueue(Long departmentId) {
        List<QueueTicket> tickets;
        if (departmentId != null) {
            tickets = ticketRepository.findByDepartmentIdAndStatusOrderBySequenceNumberAsc(departmentId, TicketStatus.WAITING);
        } else {
            tickets = ticketRepository.findByStatusOrderBySequenceNumberAsc(TicketStatus.WAITING);
        }

        return tickets.stream()
                .map(t -> QueueTicketResponse.fromEntity(t, calculateWaitingAhead(t)))
                .collect(Collectors.toList());
    }

    public List<QueueTicketResponse> getNowServingTickets() {
        return ticketRepository.findByStatusOrderBySequenceNumberAsc(TicketStatus.SERVING)
                .stream()
                .map(t -> QueueTicketResponse.fromEntity(t, 0))
                .collect(Collectors.toList());
    }

    public QueueTicketResponse getTicketByNumber(String ticketNumber) {
        QueueTicket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNumber));

        return QueueTicketResponse.fromEntity(ticket, calculateWaitingAhead(ticket));
    }

    private int calculateWaitingAhead(QueueTicket ticket) {
        if (ticket.getStatus() != TicketStatus.WAITING) {
            return 0;
        }
        return (int) ticketRepository.countByDepartmentIdAndStatusAndSequenceNumberLessThan(
                ticket.getDepartment().getId(),
                TicketStatus.WAITING,
                ticket.getSequenceNumber()
        );
    }

    private void broadcastQueueUpdate(String eventType, QueueTicket ticket) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now());
            payload.put("ticket", QueueTicketResponse.fromEntity(ticket, calculateWaitingAhead(ticket)));
            payload.put("counterNumber", ticket.getCounter() != null ? ticket.getCounter().getCounterNumber() : null);

            // Broadcast to all clients connected to /topic/queue-updates
            messagingTemplate.convertAndSend("/topic/queue-updates", payload);

            if (ticket.getCounter() != null) {
                messagingTemplate.convertAndSend("/topic/counter/" + ticket.getCounter().getId(), payload);
            }
        } catch (Exception ex) {
            log.error("Failed to broadcast WebSocket message: {}", ex.getMessage());
        }
    }
}
