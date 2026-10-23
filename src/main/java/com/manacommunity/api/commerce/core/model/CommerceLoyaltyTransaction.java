package com.manacommunity.api.commerce.core.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_loyalty_transaction")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommerceLoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private CommerceLoyaltyAccount account;

    @Column(name = "order_number", length = 40)
    private String orderNumber;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Long points;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
