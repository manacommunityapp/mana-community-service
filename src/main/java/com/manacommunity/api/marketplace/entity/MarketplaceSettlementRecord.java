package com.manacommunity.api.marketplace.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_settlement_records", indexes = {
    @Index(name = "idx_mkt_settle_seller", columnList = "seller_id"),
    @Index(name = "idx_mkt_settle_status", columnList = "status"),
    @Index(name = "idx_mkt_settle_order", columnList = "order_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceSettlementRecord extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_reference", nullable = false, unique = true, length = 100)
    private String settlementReference;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_number", nullable = false, length = 100)
    private String orderNumber;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(name = "gross_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "platform_fee", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal platformFee = BigDecimal.ZERO;

    @Column(name = "tax_deduction", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal taxDeduction = BigDecimal.ZERO;

    @Column(name = "net_payout_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal netPayoutAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_mode", nullable = false, length = 50)
    @Builder.Default
    private PayoutMode payoutMode = PayoutMode.WALLET;

    @Column(name = "payout_reference", length = 100)
    private String payoutReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING_CLEARANCE;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    public enum PayoutMode {
        WALLET,
        BANK_TRANSFER,
        UPI,
        MANUAL_CHEQUE
    }

    public enum SettlementStatus {
        PENDING_CLEARANCE,
        SCHEDULED,
        PROCESSED,
        SETTLED,
        FAILED,
        REVERSED
    }
}
