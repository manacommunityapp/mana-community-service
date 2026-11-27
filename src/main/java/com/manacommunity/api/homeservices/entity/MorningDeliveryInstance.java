package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "morning_delivery_instances",
    uniqueConstraints = @UniqueConstraint(name = "uq_morning_instance_plan_date", columnNames = {"plan_id", "delivery_date"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MorningDeliveryInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "sku_code", nullable = false, length = 64)
    private String skuCode;

    @Column(name = "tower_block", length = 32)
    private String towerBlock;

    @Builder.Default
    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber = 0;

    @Column(name = "flat_number", nullable = false, length = 32)
    private String flatNumber;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer qty = 1;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "wallet_hold_ref", length = 128)
    private String walletHoldRef;

    @Builder.Default
    @Column(name = "wallet_status", nullable = false, length = 32)
    private String walletStatus = "RESERVED"; // RESERVED, CAPTURED, AUTO_REFUNDED, LOW_BALANCE_PAUSED

    @Builder.Default
    @Column(name = "delivery_status", nullable = false, length = 32)
    private String deliveryStatus = "FROZEN_FOR_DISPATCH"; // FROZEN_FOR_DISPATCH, DELIVERED, OUT_OF_STOCK, SKIPPED

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Builder.Default
    @Column(name = "frozen_at", nullable = false)
    private Instant frozenAt = Instant.now();

    @Column(name = "settled_at")
    private Instant settledAt;
}
