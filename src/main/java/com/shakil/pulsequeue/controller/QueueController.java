package com.shakil.pulsequeue.controller;

import com.shakil.pulsequeue.dto.CallTicketRequest;
import com.shakil.pulsequeue.dto.QueueTicketResponse;
import com.shakil.pulsequeue.model.ServiceCounter;
import com.shakil.pulsequeue.repository.ServiceCounterRepository;
import com.shakil.pulsequeue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/queue")
@Tag(name = "Queue Management", description = "Real-time queue operations and waiting ticket triage")
public class QueueController {

    private final QueueService queueService;
    private final ServiceCounterRepository counterRepository;

    public QueueController(QueueService queueService, ServiceCounterRepository counterRepository) {
        this.queueService = queueService;
        this.counterRepository = counterRepository;
    }

    @GetMapping("/active")
    @Operation(summary = "Get list of waiting queue tickets")
    public ResponseEntity<List<QueueTicketResponse>> getActiveQueue(@RequestParam(required = false) Long departmentId) {
        return ResponseEntity.ok(queueService.getActiveQueue(departmentId));
    }

    @GetMapping("/serving")
    @Operation(summary = "Get all tickets currently being served across counters (for TV display)")
    public ResponseEntity<List<QueueTicketResponse>> getServing() {
        return ResponseEntity.ok(queueService.getNowServingTickets());
    }

    @GetMapping("/ticket/{ticketNumber}")
    @Operation(summary = "Query status and queue position for a ticket number")
    public ResponseEntity<QueueTicketResponse> getTicket(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(queueService.getTicketByNumber(ticketNumber));
    }

    @GetMapping("/counters")
    @Operation(summary = "Get all active service counters")
    public ResponseEntity<List<ServiceCounter>> getCounters() {
        return ResponseEntity.ok(counterRepository.findByActiveTrue());
    }

    @PostMapping("/walkin")
    @Operation(summary = "Issue a walk-in queue ticket for quick triage")
    public ResponseEntity<QueueTicketResponse> issueWalkinTicket(@RequestParam Long departmentId) {
        return ResponseEntity.ok(queueService.issueWalkInTicket(departmentId));
    }

    @PostMapping("/call")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    @Operation(summary = "Staff Action: Call next waiting customer to counter (Broadcasts WebSocket event)")
    public ResponseEntity<QueueTicketResponse> callNext(@Valid @RequestBody CallTicketRequest request) {
        return ResponseEntity.ok(queueService.callNextTicket(request.getCounterId()));
    }

    @PostMapping("/serve/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    @Operation(summary = "Staff Action: Mark ticket as actively serving")
    public ResponseEntity<QueueTicketResponse> startServe(@PathVariable Long ticketId) {
        return ResponseEntity.ok(queueService.startServing(ticketId));
    }

    @PostMapping("/complete/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    @Operation(summary = "Staff Action: Complete customer consultation")
    public ResponseEntity<QueueTicketResponse> complete(@PathVariable Long ticketId) {
        return ResponseEntity.ok(queueService.completeService(ticketId));
    }

    @PostMapping("/skip/{ticketId}")
    @PreAuthorize("hasAnyAuthority('ROLE_STAFF', 'ROLE_ADMIN')")
    @Operation(summary = "Staff Action: Mark customer as no-show / skipped")
    public ResponseEntity<QueueTicketResponse> skip(@PathVariable Long ticketId) {
        return ResponseEntity.ok(queueService.markNoShow(ticketId));
    }
}
