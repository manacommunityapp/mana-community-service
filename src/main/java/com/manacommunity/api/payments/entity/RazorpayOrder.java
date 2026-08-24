package com.manacommunity.api.payments.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistent record of a single Razorpay payment order and its lifecycle.
 *
 * <p>One row per Razorpay order. Multiple business modules (event registration,
 * pooja booking, donation, food, auction, sports, etc.) share this table via
 * {@link #referenceType} and {@link #referenceId}. This keeps all payment
 * concerns in one place and allows finance reporting without touching individual
 * module tables.
 *
 * <p>Status transitions:
 * <pre>
 *   CREATED → PAID (via /verify or webhook payment.captured)
 *   CREATED → FAILED (via invalid signature or webhook payment.failed)
 *   PAID    → REFUNDED (via webhook refund.processed)
 * </pre>
 */
@Entity
@Table(
    name = "razorpay_order",
    indexes = {
        @Index(name = "idx_rzp_order_user_community",  columnList = "user_id, community_id, created_at"),
        @Index(name = "idx_rzp_order_community_status", columnList = "community_id, status, created_at"),
        @Index(name = "idx_rzp_order_reference",        columnList = "reference_type, reference_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Razorpay's own order identifier, e.g. "order_Abc123xyz". */
    @Column(name = "razorpay_order_id", nullable = false, unique = true, length = 60)
    private String razorpayOrderId;

    /** Populated after checkout success, e.g. "pay_XyzAbc123". */
    @Column(name = "razorpay_payment_id", length = 60)
    private String razorpayPaymentId;

    /** HMAC-SHA256 signature from Razorpay checkout, stored after server-side verification. */
    @Column(name = "razorpay_signature", length = 512)
    private String razorpaySignature;

    /** Amount in INR rupees (precision 12, scale 2). Converted to paise (×100) only at the API boundary. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "CREATED";

    /**
     * Discriminates which module created this order.
     * Examples: EVENT_REGISTRATION, POOJA, DONATION, FOOD_ORDER, AUCTION,
     * SPORTS_REGISTRATION, CULTURAL_REGISTRATION, OTHER.
     */
    @Column(name = "reference_type", length = 60)
    private String referenceType;

    /** PK of the referenced business entity (CommunityEvent.id, PoojaBooking.id, etc.). */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Short description shown in the Razorpay checkout modal. */
    @Column(length = 500)
    private String description;

    /** JSON snapshot of Razorpay order notes (stored for audit). */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /** Set when status transitions to PAID. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Human-readable Razorpay error description when status = FAILED. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Razorpay idempotency key from the webhook body (event.id).
     * Checked before processing to skip duplicate webhook deliveries.
     */
    @Column(name = "webhook_event_id", length = 100, unique = true)
    private String webhookEventId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Optimistic locking — prevents concurrent status overwrites. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
