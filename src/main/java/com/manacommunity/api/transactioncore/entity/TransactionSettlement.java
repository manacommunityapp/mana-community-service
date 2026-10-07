package com.manacommunity.api.transactioncore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_settlement", indexes = {
        @Index(name = "idx_settlement_batch", columnList = "batch_id"),
        @Index(name = "idx_settlement_payee", columnList = "payee_id,payee_type"),
        @Index(name = "idx_settlement_intent", columnList = "intent_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_id", nullable = false, unique = true, length = 64)
    private String settlementId;

    @Column(name = "batch_id", length = 64)
    private String batchId;

    @Column(name = "intent_id", nullable = false, length = 64)
    private String intentId;

    @Column(name = "payee_id", nullable = false)
    private Long payeeId;

    @Column(name = "payee_type", nullable = false, length = 32)
    private String payeeType;

    @Column(name = "payee_account", length = 64)
    private String payeeAccount;

    @Column(name = "gross_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "commission_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Column(name = "tds_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal tdsAmount = BigDecimal.ZERO;

    @Column(name = "net_payout_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPayoutAmount;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Column(name = "journal_entry_number", length = 64)
    private String journalEntryNumber;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
