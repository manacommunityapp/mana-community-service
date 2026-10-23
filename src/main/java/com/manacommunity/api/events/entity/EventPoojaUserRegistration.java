package com.manacommunity.api.events.entity;

import com.manacommunity.api.events.enums.RegistrationSource;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_pooja_user_registrations")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPoojaUserRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_code", unique = true, length = 100, nullable = false)
    private String regCode;

    @Column(name = "event_id")
    private Long eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "participant_name", nullable = false, length = 255)
    private String participantName;

    @Column(name = "gotram", length = 100)
    private String gotram;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "flat_no", length = 100)
    private String flatNo;

    @Column(name = "devotee_count")
    @Builder.Default
    private Integer devoteeCount = 1;

    @Column(name = "attending_devotees", columnDefinition = "TEXT")
    private String attendingDevotees;

    @Column(name = "pooja_slot_name", length = 255)
    private String poojaSlotName;

    @Column(name = "pooja_slot_date", length = 50)
    private String poojaSlotDate;

    @Column(name = "pooja_slot_time", length = 100)
    private String poojaSlotTime;

    @Column(name = "venue", length = 255)
    private String venue;

    @Column(name = "mandap", length = 255)
    private String mandap;

    @Column(name = "pandit_name", length = 255)
    private String panditName;

    @Column(name = "category", length = 100)
    @Builder.Default
    private String category = "Pooja";

    @Column(name = "booking_fee")
    @Builder.Default
    private Double bookingFee = 0.0;

    @Column(name = "payment_status", length = 50)
    @Builder.Default
    private String paymentStatus = "FREE";

    @Column(name = "payment_method", length = 100)
    @Builder.Default
    private String paymentMethod = "Free Seva";

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "payment_receipt_url", columnDefinition = "TEXT")
    private String paymentReceiptUrl;

    @Column(name = "prasadam_mode", length = 50)
    @Builder.Default
    private String prasadamMode = "mandap";

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "qr_code_url", columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** FK to event_pooja_schedule — set when the new booking engine is used. Null for legacy registrations. */
    @Column(name = "schedule_id")
    private Long scheduleId;

    /** FK to event_pooja_slot_reservation — set when slot was pre-reserved. Null for legacy / admin registrations. */
    @Column(name = "reservation_id")
    private Long reservationId;

    /** FK to event_pooja_seva_time_slots — the specific time slot config row the devotee booked. */
    @Column(name = "pooja_seva_time_slots_id")
    private Long poojaSevaTimeSlotsId;

    /** Denormalized FK to event_pooja_sevas — set at create time from the time slot or schedule so
     *  registrations can be filtered by pooja seva without joining through intermediate tables. */
    @Column(name = "pooja_seva_id")
    private Long poojaSevaId;

    public Long getPoojaSevaId() { return poojaSevaId; }
    public void setPoojaSevaId(Long poojaSevaId) { this.poojaSevaId = poojaSevaId; }

    /** Token number assigned at reservation time — persisted here so it remains available after the reservation row is purged. */
    @Column(name = "token_number")
    private Integer tokenNumber;

    // ── Admin / audit fields ──────────────────────────────────────────────────

    /** How this booking was created: SELF (normal user), ADMIN (on behalf), IMPORT (bulk). */
    @Enumerated(EnumType.STRING)
    @Column(name = "registration_source", length = 20, nullable = false)
    @Builder.Default
    private RegistrationSource registrationSource = RegistrationSource.SELF;

    /** ID of the admin who created this booking when registrationSource = ADMIN. Null for SELF bookings. */
    @Column(name = "registered_by")
    private Long registeredBy;

    /** True when the admin bypassed capacity or duplicate-registration checks (adminOverride=true). */
    @Column(name = "override_used", nullable = false)
    @Builder.Default
    private Boolean overrideUsed = false;

    /** Free-text reason the admin gave for overriding normal booking rules. */
    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    /** Individual devotees for this booking — authoritative replacement for the attendingDevotees JSON blob. */
    @OneToMany(mappedBy = "registration", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<EventPoojaBookingParticipant> participants = new java.util.ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;

    public Long getPoojaSevaTimeSlotsId() { return poojaSevaTimeSlotsId; }
    public void setPoojaSevaTimeSlotsId(Long poojaSevaTimeSlotsId) { this.poojaSevaTimeSlotsId = poojaSevaTimeSlotsId; }

    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        if (this.overrideUsed == null) {
            this.overrideUsed = false;
        }
        if (this.registrationSource == null) {
            this.registrationSource = RegistrationSource.SELF;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
