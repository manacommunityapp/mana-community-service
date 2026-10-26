package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_bids")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rfq_id", nullable = false)
    private Long rfqId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName;

    @Column(name = "bid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal bidAmount;

    @Column(name = "delivery_days")
    private Integer deliveryDays;

    @Column(length = 500)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AuctionBidStatus status = AuctionBidStatus.ACTIVE;

    @Column(name = "rank_position")
    private Integer rankPosition;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "created_at", nullable = false, updatable = false)
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
