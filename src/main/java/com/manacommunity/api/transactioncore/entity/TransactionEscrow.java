package com.manacommunity.api.transactioncore.entity;

import com.manacommunity.api.transactioncore.enums.EscrowReleaseCondition;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_escrow", indexes = {
        @Index(name = "idx_escrow_intent", columnList = "intent_id"),
        @Index(name = "idx_escrow_status", columnList = "status"),
        @Index(name = "idx_escrow_payee", columnList = "payee_id,payee_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEscrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escrow_id", nullable = false, unique = true, length = 64)
    private String escrowId;

    @Column(name = "intent_id", nullable = false, length = 64)
    private String intentId;

    @Column(name = "held_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal heldAmount;

    @Column(name = "released_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal releasedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_condition", nullable = false, length = 32)
    private EscrowReleaseCondition releaseCondition;

    @Column(name = "verification_code", length = 32)
    private String verificationCode;

    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private String status = "HELD";

    @Column(name = "payee_id")
    private Long payeeId;

    @Column(name = "payee_type", length = 32)
    private String payeeType;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "release_notes", length = 255)
    private String releaseNotes;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "released_at")
    private LocalDateTime releasedAt;
}
