package com.manacommunity.api.groupbuying.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_buy_settlement")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupDealSettlement extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private GroupDeal deal;

    @Column(name = "deal_title", nullable = false)
    private String dealTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "vendor_id", nullable = false)
    private String vendorId;

    @Column(name = "vendor_name", nullable = false)
    private String vendorName;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "gross_sales_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal grossSalesAmount;

    @Column(name = "platform_commission_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal platformCommissionRate;

    @Column(name = "platform_commission_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal platformCommissionAmount;

    @Column(name = "community_reserve_rate", precision = 5, scale = 2, nullable = false)
    private BigDecimal communityReserveRate;

    @Column(name = "community_reserve_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal communityReserveAmount;

    @Column(name = "tier_refunds_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal tierRefundsTotal;

    @Column(name = "net_vendor_payout", precision = 12, scale = 2, nullable = false)
    private BigDecimal netVendorPayout;

    @Column(name = "settlement_status", length = 30, nullable = false)
    @Builder.Default
    private String settlementStatus = "PENDING"; // PENDING, PROCESSING, SETTLED, DISPUTED

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}
