package com.shakil.pulsequeue.dto;

import com.shakil.pulsequeue.model.QueueTicket;
import com.shakil.pulsequeue.model.TicketStatus;
import java.time.LocalDateTime;

public class QueueTicketResponse {

    private Long id;
    private String ticketNumber;
    private String customerName;
    private String departmentName;
    private String counterNumber;
    private TicketStatus status;
    private int sequenceNumber;
    private LocalDateTime issuedAt;
    private LocalDateTime calledAt;
    private int waitingAheadCount;

    public QueueTicketResponse() {}

    public static QueueTicketResponse fromEntity(QueueTicket ticket, int waitingAheadCount) {
        QueueTicketResponse res = new QueueTicketResponse();
        res.setId(ticket.getId());
        res.setTicketNumber(ticket.getTicketNumber());
        res.setDepartmentName(ticket.getDepartment().getName());
        if (ticket.getAppointment() != null && ticket.getAppointment().getCustomer() != null) {
            res.setCustomerName(ticket.getAppointment().getCustomer().getFullName());
        } else {
            res.setCustomerName("Walk-in Patient");
        }
        if (ticket.getCounter() != null) {
            res.setCounterNumber(ticket.getCounter().getCounterNumber());
        }
        res.setStatus(ticket.getStatus());
        res.setSequenceNumber(ticket.getSequenceNumber());
        res.setIssuedAt(ticket.getIssuedAt());
        res.setCalledAt(ticket.getCalledAt());
        res.setWaitingAheadCount(waitingAheadCount);
        return res;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getCounterNumber() { return counterNumber; }
    public void setCounterNumber(String counterNumber) { this.counterNumber = counterNumber; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getCalledAt() { return calledAt; }
    public void setCalledAt(LocalDateTime calledAt) { this.calledAt = calledAt; }

    public int getWaitingAheadCount() { return waitingAheadCount; }
    public void setWaitingAheadCount(int waitingAheadCount) { this.waitingAheadCount = waitingAheadCount; }
}
