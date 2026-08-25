package com.manacommunity.api.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class EventPoojaSevaDayTimeSlot {

    @Column(name = "slot_date")
    private LocalDate slotDate; // null for single-day events

    @Column(name = "start_time", length = 20)
    private String startTime; // e.g. "08:30"

    @Column(name = "title", length = 200)
    private String title; // slot name / title e.g. "Morning Abhishekam"

    @Column(name = "slot_count")
    private Integer slotCount;

    public EventPoojaSevaDayTimeSlot() {}

    public EventPoojaSevaDayTimeSlot(LocalDate slotDate, String startTime, Integer slotCount) {
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.slotCount = slotCount;
    }

    public EventPoojaSevaDayTimeSlot(LocalDate slotDate, String startTime, String title, Integer slotCount) {
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.title = title;
        this.slotCount = slotCount;
    }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getSlotCount() { return slotCount; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount; }
}
