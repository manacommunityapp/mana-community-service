package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodWalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private FoodWallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private WalletTransactionType transactionType;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String description;

    @Column(name = "balance_after", precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum WalletTransactionType {
        CREDIT, DEBIT, REFUND, CASHBACK
    }
}
