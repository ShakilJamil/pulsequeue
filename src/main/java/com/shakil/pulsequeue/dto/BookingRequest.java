package com.shakil.pulsequeue.dto;

import jakarta.validation.constraints.NotNull;

public class BookingRequest {

    @NotNull(message = "Time slot ID is required")
    private Long slotId;

    private String notes;

    public BookingRequest() {}

    public BookingRequest(Long slotId, String notes) {
        this.slotId = slotId;
        this.notes = notes;
    }

    public Long getSlotId() { return slotId; }
    public void setSlotId(Long slotId) { this.slotId = slotId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
