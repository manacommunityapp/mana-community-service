package com.manacommunity.api.transactioncore.entity;

import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import com.manacommunity.api.transactioncore.enums.PaymentRail;
import com.manacommunity.api.transactioncore.enums.TransactionDomain;
import com.manacommunity.api.transactioncore.enums.TransactionIntentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_intent", indexes = {
        @Index(name = "idx_txn_intent_id", columnList = "intent_id", unique = true),
        @Index(name = "idx_txn_idempotency_key", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_txn_order_ref", columnList = "order_reference_id"),
        @Index(name = "idx_txn_payer", columnList = "payer_id"),
        @Index(name = "idx_txn_domain_status", columnList = "domain,status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intent_id", nullable = false, unique = true, length = 64)
    private String intentId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionDomain domain;

    @Column(name = "order_reference_id", nullable = false, length = 64)
    private String orderReferenceId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(name = "payer_name", length = 150)
    private String payerName;

    @Column(name = "payer_email", length = 150)
    private String payerEmail;

    @Column(name = "payer_phone", length = 30)
    private String payerPhone;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    @Builder.Default
    private TransactionIntentStatus status = TransactionIntentStatus.INITIATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_rail", length = 32)
    @Builder.Default
    private PaymentRail paymentRail = PaymentRail.UPI;

    @Column(name = "wallet_deduction_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal walletDeductionAmount = BigDecimal.ZERO;

    @Column(name = "gateway_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gatewayAmount = BigDecimal.ZERO;

    @Column(name = "gateway_order_id", length = 100)
    private String gatewayOrderId;

    @Column(name = "gateway_payment_id", length = 100)
    private String gatewayPaymentId;

    @Column(name = "escrow_held")
    @Builder.Default
    private boolean escrowHeld = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "escrow_condition", length = 32)
    private EscrowReleaseCondition escrowReleaseCondition;

    @Column(name = "journal_entry_number", length = 64)
    private String journalEntryNumber;

    @Column(name = "receipt_number", length = 64)
    private String receiptNumber;

    @Column(name = "invoice_number", length = 64)
    private String invoiceNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "splits_json", columnDefinition = "jsonb")
    private String splitsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
