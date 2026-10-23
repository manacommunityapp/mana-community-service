package com.manacommunity.api.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class EventPoojaSevaDaySlot {

    @Column(name = "slot_date")
    private LocalDate slotDate;

    @Column(name = "slot_count")
    private Integer slotCount;

    public EventPoojaSevaDaySlot() {}

    public EventPoojaSevaDaySlot(LocalDate slotDate, Integer slotCount) {
        this.slotDate = slotDate;
        this.slotCount = slotCount;
    }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public Integer getSlotCount() { return slotCount; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount; }
}
