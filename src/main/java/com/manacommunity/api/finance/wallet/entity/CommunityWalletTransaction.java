package com.manacommunity.api.finance.wallet.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "community_wallet_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunityWalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "txn_type", nullable = false, length = 50)
    private String txnType;

    @Column(nullable = false, length = 10)
    private String direction;  // DEBIT, CREDIT

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String description;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "external_txn_ref", length = 200)
    private String externalTxnRef;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "initiated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime initiatedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
}
