package com.manacommunity.api.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

@Embeddable
public class EventPoojaSevaDayTimeSlot {

    @Column(name = "id", insertable = false, updatable = false)
    private Long id;

    @Column(name = "slot_date")
    private LocalDate slotDate;

    @Column(name = "start_time", length = 20)
    private String startTime;

    @Column(name = "end_time", length = 20)
    private String endTime;

    @Column(name = "title", length = 200)
    private String title;

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

    public EventPoojaSevaDayTimeSlot(LocalDate slotDate, String startTime, String endTime, String title, Integer slotCount) {
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.slotCount = slotCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getSlotDate() { return slotDate; }
    public void setSlotDate(LocalDate slotDate) { this.slotDate = slotDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getSlotCount() { return slotCount; }
    public void setSlotCount(Integer slotCount) { this.slotCount = slotCount; }
}
