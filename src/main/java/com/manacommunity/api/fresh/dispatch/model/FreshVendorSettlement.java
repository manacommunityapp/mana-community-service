package com.manacommunity.api.fresh.dispatch.model;

import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fresh_vendor_settlement", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshVendorSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "settlement_reference", nullable = false, unique = true, length = 80)
    private String settlementReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "settlement_period_start", nullable = false)
    private LocalDate settlementPeriodStart;

    @Column(name = "settlement_period_end", nullable = false)
    private LocalDate settlementPeriodEnd;

    @Column(name = "gross_sales_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal grossSalesAmount = BigDecimal.ZERO;

    @Column(name = "mana_commission_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal manaCommissionAmount = BigDecimal.ZERO;

    @Column(name = "delivery_fees_collected", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFeesCollected = BigDecimal.ZERO;

    @Column(name = "gateway_fees_deducted", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gatewayFeesDeducted = BigDecimal.ZERO;

    @Column(name = "tds_deducted_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal tdsDeductedAmount = BigDecimal.ZERO;

    @Column(name = "gst_on_commission_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal gstOnCommissionAmount = BigDecimal.ZERO;

    @Column(name = "refund_clawbacks_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal refundClawbacksAmount = BigDecimal.ZERO;

    @Column(name = "net_payable_amount", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal netPayableAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "payout_utr_reference", length = 100)
    private String payoutUtrReference;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}