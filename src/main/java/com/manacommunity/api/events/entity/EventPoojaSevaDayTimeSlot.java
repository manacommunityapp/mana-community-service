package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "event_pooja_seva_time_slots")
public class EventPoojaSevaDayTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pooja_seva_id", nullable = false)
    private Long poojaSevaId;

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

    public EventPoojaSevaDayTimeSlot(Long poojaSevaId, LocalDate slotDate, String startTime, Integer slotCount) {
        this.poojaSevaId = poojaSevaId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.slotCount = slotCount;
    }

    public EventPoojaSevaDayTimeSlot(Long poojaSevaId, LocalDate slotDate, String startTime, String endTime, String title, Integer slotCount) {
        this.poojaSevaId = poojaSevaId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.slotCount = slotCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPoojaSevaId() { return poojaSevaId; }
    public void setPoojaSevaId(Long poojaSevaId) { this.poojaSevaId = poojaSevaId; }

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
