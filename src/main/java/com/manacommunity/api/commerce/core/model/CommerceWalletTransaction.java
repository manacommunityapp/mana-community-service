package com.manacommunity.api.commerce.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_wallet_transaction")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CommerceWalletTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CommerceWallet wallet;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 60)
    private String txnRef;

    @Column(name = "txn_type", nullable = false, length = 20)
    private String txnType;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "reference_id", length = 80)
    private String referenceId;

    @Column(length = 300)
    private String description;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
