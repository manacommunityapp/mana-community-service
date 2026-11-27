package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "multi_tender_settlements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiTenderSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_ref", nullable = false, unique = true, length = 64)
    private String orderRef;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "vendor_id", nullable = false)
    private Long vendorId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Builder.Default
    @Column(name = "points_used", nullable = false, precision = 12, scale = 2)
    private BigDecimal pointsUsed = BigDecimal.ZERO;

    @Column(name = "coupon_code", length = 64)
    private String couponCode;

    @Builder.Default
    @Column(name = "coupon_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "upi_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal upiAmount = BigDecimal.ZERO;

    @Column(name = "platform_commission", nullable = false, precision = 12, scale = 2)
    private BigDecimal platformCommission;

    @Column(name = "commission_gst", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionGst;

    @Column(name = "vendor_net_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal vendorNetPayout;

    @Builder.Default
    @Column(nullable = false, length = 32)
    private String state = "INITIATED"; // INITIATED, FUNDS_RESERVED, CAPTURED_IN_ESCROW, SETTLEMENT_ELIGIBLE, COMPENSATED

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
