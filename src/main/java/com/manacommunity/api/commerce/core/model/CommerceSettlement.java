package com.manacommunity.api.commerce.core.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "commerce_settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommerceSettlement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_number", unique = true, nullable = false, length = 64)
    private String settlementNumber;

    @Column(name = "vendor_id", length = 64)
    private String vendorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private AppUser seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "cycle_start_date", nullable = false)
    private Instant cycleStartDate;

    @Column(name = "cycle_end_date", nullable = false)
    private Instant cycleEndDate;

    @Column(name = "total_orders_count", nullable = false)
    @Builder.Default
    private Integer totalOrdersCount = 0;

    @Column(name = "gross_amount", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "platform_fee", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "deductions", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal deductions = BigDecimal.ZERO;

    @Column(name = "net_payout", precision = 12, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal netPayout = BigDecimal.ZERO;

    @Column(nullable = false, length = 32)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "payout_utr", length = 64)
    private String payoutUtr;

    @Column(name = "payout_date")
    private Instant payoutDate;
}