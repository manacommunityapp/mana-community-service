package com.manacommunity.api.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class PoojaSevaDayTimeSlot {

    @Column(name = "slot_date")
    private LocalDate slotDate; // null for single-day events

    @Column(name = "start_time", length = 20)
    private String startTime; // e.g. "08:30"

    @Column(name = "slot_count")
    private Integer slotCount;

    public PoojaSevaDayTimeSlot() {}

    public PoojaSevaDayTimeSlot(LocalDate slotDate, String startTime, Integer slotCount) {
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.slotCount = slotCount;
    }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public Integer getSlotCount() { return slotCount; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount; }
}
