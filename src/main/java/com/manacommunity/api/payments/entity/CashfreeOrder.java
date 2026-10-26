package com.manacommunity.api.payments.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistent record of a single Cashfree payment order and its lifecycle.
 *
 * <p>One row per Cashfree order. All business modules (sports registration,
 * event tickets, venue bookings, marketplace, etc.) share this table via
 * {@link #referenceType} and {@link #referenceId}.
 *
 * <p>Status transitions:
 * <pre>
 *   CREATED → PAID (via /verify or webhook PAYMENT_SUCCESS)
 *   CREATED → FAILED (via /verify failure or webhook PAYMENT_FAILED)
 *   CREATED → EXPIRED (via webhook ORDER_EXPIRED)
 *   PAID    → REFUND_PENDING → REFUNDED (via refund workflow)
 * </pre>
 */
@Entity
@Table(
    name = "cashfree_order",
    indexes = {
        @Index(name = "idx_cf_order_user_community",  columnList = "user_id, community_id, created_at"),
        @Index(name = "idx_cf_order_community_status", columnList = "community_id, status, created_at"),
        @Index(name = "idx_cf_order_reference",        columnList = "reference_type, reference_id"),
        @Index(name = "idx_cf_order_mana_order_id",    columnList = "mana_order_id")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashfreeOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mana's own order identifier sent to Cashfree as order_id.
     * Format: MANA-{epochMs}-{userId}. Max 50 chars per Cashfree docs.
     */
    @Column(name = "mana_order_id", nullable = false, unique = true, length = 50)
    private String manaOrderId;

    /** Cashfree's internal numeric order ID (cf_order_id), stored for reconciliation. */
    @Column(name = "cf_order_id", length = 40)
    private String cfOrderId;

    /**
     * Cashfree payment_session_id returned from order creation.
     * Passed to the mobile/web SDK to open the checkout sheet.
     * Never logged in plain text; access is restricted to this service.
     */
    @Column(name = "payment_session_id", length = 512)
    private String paymentSessionId;

    /** Cashfree payment reference ID, set after successful payment. */
    @Column(name = "cf_payment_id", length = 100)
    private String cfPaymentId;

    /** Amount in INR rupees (precision 12, scale 2). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 5)
    @Builder.Default
    private String currency = "INR";

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "CREATED";

    /**
     * Discriminates which module created this order.
     * Examples: SPORTS_REGISTRATION, EVENT_TICKET, VENUE_BOOKING,
     * MARKETPLACE_ORDER, COMMUNITY_SUBSCRIPTION.
     */
    @Column(name = "reference_type", length = 60)
    private String referenceType;

    /** PK of the referenced business entity. */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Short description shown in checkout. */
    @Column(length = 500)
    private String description;

    /** Payment method used (e.g. UPI, CARD, NETBANKING), set on success. */
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    /** Human-readable failure message when status = FAILED. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * Cashfree webhook event ID for idempotency.
     * Checked before processing to skip duplicate webhook deliveries.
     */
    @Column(name = "webhook_event_id", length = 100)
    private String webhookEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    /** Set when status transitions to PAID. */
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

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
