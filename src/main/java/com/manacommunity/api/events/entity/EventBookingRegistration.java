package com.manacommunity.api.events.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_booking_registrations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventBookingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reg_code", unique = true, length = 100)
    private String regCode;

    @Column(name = "activity_id", length = 100)
    private String activityId;

    /**
     * ID of the parent community / top-level event this registration belongs to.
     * Stored so that {@code GET /api/events/registrations/my} can return it
     * for frontend deduplication (same-pooja, different-day blocking).
     */
    @Column(name = "main_event_id")
    private Long mainEventId;

    @Column(name = "activity_title", nullable = false, length = 255)
    private String activityTitle;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "pass_type", length = 100)
    private String passType;

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

    @Column(name = "attending_devotees", columnDefinition = "TEXT")
    private String attendingDevotees;

    @Column(name = "members_json", columnDefinition = "TEXT")
    private String membersJson;

    @Column(name = "devotee_count")
    @Builder.Default
    private Integer devoteeCount = 1;

    @Column(name = "event_date", length = 50)
    private String eventDate;

    @Column(name = "event_time", length = 100)
    private String eventTime;

    @Column(name = "venue", length = 255)
    private String venue;

    @Column(name = "booking_fee")
    @Builder.Default
    private Double bookingFee = 0.0;

    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = "PAID";

    @Column(name = "payment_receipt_url", length = 1000)
    private String paymentReceiptUrl;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "CONFIRMED";

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    @Column(name = "checked_in")
    @Builder.Default
    private Boolean checkedIn = false;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
