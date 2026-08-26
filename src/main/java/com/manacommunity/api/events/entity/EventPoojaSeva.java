package com.manacommunity.api.events.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event_pooja_sevas")
@EntityListeners(AuditingEntityListener.class)
public class EventPoojaSeva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "main_event_id")
    private Long mainEventId;

    @Column(name = "pooja_type_id")
    private Long poojaTypeId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "type", nullable = false, length = 150)
    private String type;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "multi_day")
    private Boolean multiDay = false;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "mandap", length = 200)
    private String mandap;

    @Column(name = "pandit", length = 200)
    private String pandit;

    @Column(name = "slots")
    private Integer slots;

    @Column(name = "fee", precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(name = "is_free")
    private Boolean isFree = false;

    @Column(name = "needs_registration")
    private Boolean needsRegistration = true;

    @ElementCollection
    @CollectionTable(name = "event_pooja_seva_items", joinColumns = @JoinColumn(name = "pooja_seva_id"))
    @Column(name = "item_name")
    private List<String> items = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "event_pooja_seva_start_times", joinColumns = @JoinColumn(name = "pooja_seva_id"))
    @Column(name = "start_time_slot")
    private List<String> startTimes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "event_pooja_seva_day_slots", joinColumns = @JoinColumn(name = "pooja_seva_id"))
    @jakarta.persistence.OrderBy("slotDate ASC")
    private List<EventPoojaSevaDaySlot> daySlots = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "event_pooja_seva_time_slots", joinColumns = @JoinColumn(name = "pooja_seva_id"))
    @jakarta.persistence.OrderBy("slotDate ASC, startTime ASC")
    private List<EventPoojaSevaDayTimeSlot> timeSlotConfig = new ArrayList<>();

    @Column(name = "notes", length = 1000)
    private String notes;

    // ── Booking-engine configuration (V73) ──

    /** When bookings open. Null = no restriction. */
    @Column(name = "booking_open")
    private LocalDateTime bookingOpen;

    /** When bookings close. Null = no restriction. */
    @Column(name = "booking_close")
    private LocalDateTime bookingClose;

    /** Hard limit on devotees per single booking. Null = use slot devotee_capacity. */
    @Column(name = "max_devotees_per_booking")
    private Integer maxDevoteesPerBooking;

    @Column(name = "prasadam_available")
    private Boolean prasadamAvailable = false;

    @Column(name = "sankalpam_required")
    private Boolean sankalpamRequired = false;

    @Column(name = "approval_required")
    private Boolean approvalRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    public EventPoojaSeva() {
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommunityId() { return communityId; }
    public void setCommunityId(Long communityId) { this.communityId = communityId; }

    public Long getMainEventId() { return mainEventId; }
    public void setMainEventId(Long mainEventId) { this.mainEventId = mainEventId; }

    public Long getPoojaTypeId() { return poojaTypeId; }
    public void setPoojaTypeId(Long poojaTypeId) { this.poojaTypeId = poojaTypeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Boolean getMultiDay() { return multiDay; }
    public void setMultiDay(Boolean multiDay) { this.multiDay = multiDay; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getMandap() { return mandap; }
    public void setMandap(String mandap) { this.mandap = mandap; }

    public String getPandit() { return pandit; }
    public void setPandit(String pandit) { this.pandit = pandit; }

    public Integer getSlots() { return slots; }
    public void setSlots(Integer slots) { this.slots = slots; }

    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }

    public Boolean getIsFree() { return isFree; }
    public void setIsFree(Boolean isFree) { this.isFree = isFree; }

    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }

    public List<String> getStartTimes() { return startTimes; }
    public void setStartTimes(List<String> startTimes) {
        this.startTimes = startTimes != null ? startTimes : new ArrayList<>();
        if (this.startTime == null && !this.startTimes.isEmpty()) {
            try {
                this.startTime = LocalTime.parse(this.startTimes.get(0));
            } catch (Exception ignored) {}
        }
    }

    public List<EventPoojaSevaDaySlot> getDaySlots() { return daySlots; }
    public void setDaySlots(List<EventPoojaSevaDaySlot> daySlots) {
        this.daySlots = daySlots != null ? daySlots : new ArrayList<>();
    }

    public List<EventPoojaSevaDayTimeSlot> getTimeSlotConfig() { return timeSlotConfig; }
    public void setTimeSlotConfig(List<EventPoojaSevaDayTimeSlot> timeSlotConfig) {
        this.timeSlotConfig = timeSlotConfig != null ? timeSlotConfig : new ArrayList<>();
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getBookingOpen() { return bookingOpen; }
    public void setBookingOpen(LocalDateTime bookingOpen) { this.bookingOpen = bookingOpen; }
    public LocalDateTime getBookingClose() { return bookingClose; }
    public void setBookingClose(LocalDateTime bookingClose) { this.bookingClose = bookingClose; }
    public Integer getMaxDevoteesPerBooking() { return maxDevoteesPerBooking; }
    public void setMaxDevoteesPerBooking(Integer v) { this.maxDevoteesPerBooking = v; }
    public Boolean getPrasadamAvailable() { return prasadamAvailable; }
    public void setPrasadamAvailable(Boolean v) { this.prasadamAvailable = v; }
    public Boolean getSankalpamRequired() { return sankalpamRequired; }
    public void setSankalpamRequired(Boolean v) { this.sankalpamRequired = v; }
    public Boolean getApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(Boolean v) { this.approvalRequired = v; }

    public Boolean getNeedsRegistration() { return needsRegistration; }
    public void setNeedsRegistration(Boolean needsRegistration) { this.needsRegistration = needsRegistration; }
}

