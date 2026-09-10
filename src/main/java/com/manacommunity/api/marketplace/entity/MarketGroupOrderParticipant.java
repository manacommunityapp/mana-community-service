package com.manacommunity.api.marketplace.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "marketplace_group_order_participants", indexes = {
    @Index(name = "idx_mkt_part_grp", columnList = "group_order_id"),
    @Index(name = "idx_mkt_part_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketGroupOrderParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_order_id", nullable = false)
    @JsonIgnoreProperties({"participants", "discountTiers"})
    private MarketGroupOrder groupOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "locked_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal lockedPrice;

    @Column(name = "total_paid", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPaid;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (paymentStatus == null) paymentStatus = PaymentStatus.PENDING;
    }

    public enum PaymentStatus { PENDING, PAID, REFUNDED }
}
