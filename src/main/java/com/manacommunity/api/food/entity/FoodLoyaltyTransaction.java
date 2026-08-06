package com.manacommunity.api.food.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "food_loyalty_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodLoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private FoodLoyaltyMember member;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 20)
    private LoyaltyTransactionType transactionType;

    @Column
    private Integer points;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String description;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum LoyaltyTransactionType { EARN, REDEEM, EXPIRE, BONUS }
}
