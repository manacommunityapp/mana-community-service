package com.manacommunity.api.transaction.entity;

import com.manacommunity.api.transaction.enums.TransactionDomain;
import com.manacommunity.api.transaction.enums.TransactionPaymentMethod;
import com.manacommunity.api.transaction.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_core_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionCoreRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 64)
    private String transactionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain", nullable = false, length = 40)
    private TransactionDomain domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private TransactionPaymentMethod paymentMethod;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "net_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal netAmount;

    @Column(name = "tax_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "tds_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "vendor_payout_amount", precision = 18, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal vendorPayoutAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 10, nullable = false)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "payer_id", nullable = false)
    private Long payerId;

    @Column(name = "payee_id")
    private Long payeeId;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "reference_type", length = 64)
    private String referenceType;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "is_escrow", nullable = false)
    @Builder.Default
    private boolean isEscrow = false;

    @Column(name = "escrow_released", nullable = false)
    @Builder.Default
    private boolean escrowReleased = false;

    @Column(name = "escrow_released_at")
    private LocalDateTime escrowReleasedAt;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "receipt_id")
    private Long receiptId;

    @Column(name = "journal_entry_id")
    private Long journalEntryId;

    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "gateway_reference", length = 128)
    private String gatewayReference;

    @Column(name = "narration", columnDefinition = "TEXT")
    private String narration;

    @Column(name = "reconciliation_status", length = 30)
    @Builder.Default
    private String reconciliationStatus = "PENDING";

    @Column(name = "reconciliation_utr", length = 128)
    private String reconciliationUtr;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
