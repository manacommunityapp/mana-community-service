package com.manacommunity.api.events.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.events.enums.PoojaScheduleStatus;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "event_pooja_seva_time_slots")
public class EventPoojaSevaDayTimeSlot implements Persistable<Long> {

    private static final AtomicLong SEQ = new AtomicLong(0);

    @Id
    private Long id;

    @Transient
    @JsonIgnore
    private boolean isNew = true;

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

    /** Admin-controlled status: OPEN (bookable), BLOCKED (temporarily disabled), CLOSED (no schedule rows created). */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PoojaScheduleStatus status = PoojaScheduleStatus.OPEN;

    public EventPoojaSevaDayTimeSlot() {}

    public EventPoojaSevaDayTimeSlot(Long poojaSevaId, LocalDate slotDate, String startTime, Integer slotCount) {
        this.id = generateId();
        this.poojaSevaId = poojaSevaId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.slotCount = slotCount;
    }

    public EventPoojaSevaDayTimeSlot(Long poojaSevaId, LocalDate slotDate, String startTime, String endTime, String title, Integer slotCount) {
        this.id = generateId();
        this.poojaSevaId = poojaSevaId;
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.title = title;
        this.slotCount = slotCount;
    }

    public static long generateId() {
        long epochMs = System.currentTimeMillis();
        long counter = SEQ.incrementAndGet() % 1000;
        return epochMs * 1000 + counter;
    }

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = generateId();
        }
    }

    @PostLoad
    public void markNotNew() {
        this.isNew = false;
    }

    @Override
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    @JsonIgnore
    public boolean isNew() { return isNew || id == null; }
    public void setNew(boolean isNew) { this.isNew = isNew; }

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

    public PoojaScheduleStatus getStatus() { return status; }
    public void setStatus(PoojaScheduleStatus status) { this.status = status; }
}
