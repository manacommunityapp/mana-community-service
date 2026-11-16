package com.manacommunity.api.payments.entity;

import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Unified payment order — single source of truth for all gateway-initiated payments.
 *
 * <p>All business modules (maintenance invoices, events, sports, food, marketplace,
 * health, amenities, trips) create a row here before redirecting the payer to any PG.
 *
 * <p>Status lifecycle:
 * <pre>
 *   INITIATED → PENDING → SUCCESS → REFUND_PENDING → REFUNDED / PARTIALLY_REFUNDED
 *                       ↘ FAILED
 *                       ↘ EXPIRED
 * </pre>
 *
 * <p>Financial rules enforced at the DB level:
 * <ul>
 *   <li>amount NUMERIC(14,2) — never float</li>
 *   <li>idempotency_key UNIQUE — prevents duplicate orders</li>
 *   <li>refunded_amount ≤ amount (CHECK constraint)</li>
 *   <li>version column for optimistic locking — prevents concurrent status overwrites</li>
 * </ul>
 */
@Entity
@Table(
    name = "payment_order",
    indexes = {
        @Index(name = "idx_payment_order_community",  columnList = "community_id"),
        @Index(name = "idx_payment_order_reference",  columnList = "reference_type, reference_id"),
        @Index(name = "idx_payment_order_status",     columnList = "status"),
        @Index(name = "idx_payment_order_payer",      columnList = "payer_user_id"),
        @Index(name = "idx_payment_order_created",    columnList = "created_at")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false, updatable = false)
    private Long communityId;

    /** Caller-supplied idempotency key; UNIQUE constraint prevents duplicate orders. */
    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private PaymentGateway gateway;

    /** Gateway's own order identifier (e.g. Cashfree mana_order_id, Razorpay order_id). */
    @Column(name = "gateway_order_id", length = 128)
    private String gatewayOrderId;

    /** Gateway's payment identifier — set after capture. */
    @Column(name = "gateway_payment_id", length = 128)
    private String gatewayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, updatable = false, length = 30)
    private PaymentReferenceType referenceType;

    /** PK of the referenced business entity. */
    @Column(name = "reference_id", nullable = false, updatable = false)
    private Long referenceId;

    @Column(name = "payer_user_id", nullable = false, updatable = false)
    private Long payerUserId;

    /** Amount in INR (NUMERIC 14,2). Never float or double. */
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    @Builder.Default
    private PaymentOrderStatus status = PaymentOrderStatus.INITIATED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Total amount refunded so far. ≤ amount (enforced by DB CHECK). */
    @Column(name = "refunded_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    /** FK to the CFBOS journal entry created after capture. */
    @Column(name = "ledger_entry_id")
    private Long ledgerEntryId;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "webhook_received_at")
    private OffsetDateTime webhookReceivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** Optimistic locking — prevents concurrent status overwrites. */
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
