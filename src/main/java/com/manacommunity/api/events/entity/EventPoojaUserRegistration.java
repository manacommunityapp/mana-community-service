package com.manacommunity.api.events.entity;

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

    /** FK to pooja_schedule — set when the new booking engine is used. Null for legacy registrations. */
    @Column(name = "schedule_id")
    private Long scheduleId;

    /** FK to pooja_slot_reservation — set when slot was pre-reserved. Null for legacy / admin registrations. */
    @Column(name = "reservation_id")
    private Long reservationId;

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

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
