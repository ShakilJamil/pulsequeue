package com.shakil.pulsequeue.dto;

import jakarta.validation.constraints.NotNull;

public class CallTicketRequest {

    @NotNull(message = "Counter ID is required")
    private Long counterId;

    public CallTicketRequest() {}

    public CallTicketRequest(Long counterId) {
        this.counterId = counterId;
    }

    public Long getCounterId() { return counterId; }
    public void setCounterId(Long counterId) { this.counterId = counterId; }
}
