package com.manacommunity.api.fresh.weight.model;

import com.manacommunity.api.commerce.core.model.CommerceOrder;
import com.manacommunity.api.fresh.fulfilment.model.FreshFulfilmentItem;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_weight_adjustment_log", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreshWeightAdjustmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adjustment_reference", unique = true, nullable = false, length = 60)
    private String adjustmentReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilment_item_id", nullable = false)
    private FreshFulfilmentItem fulfilmentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CommerceOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @Column(name = "requested_weight_g", nullable = false, precision = 12, scale = 3)
    private BigDecimal requestedWeightG;

    @Column(name = "actual_weight_g", nullable = false, precision = 12, scale = 3)
    private BigDecimal actualWeightG;

    @Column(name = "weight_difference_g", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightDifferenceG;

    @Column(name = "weight_difference_pct", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightDifferencePct;

    @Column(name = "max_tolerance_pct", nullable = false, precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal maxTolerancePct = new BigDecimal("15.00");

    @Column(name = "original_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(name = "adjusted_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal adjustedAmount;

    @Column(name = "refund_or_charge_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundOrChargeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_status", nullable = false, length = 50)
    @Builder.Default
    private WeightAdjustmentStatus adjustmentStatus = WeightAdjustmentStatus.AUTO_ADJUSTED;

    @Column(length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weighed_by_user_id")
    private AppUser weighedByUser;

    @Column(name = "wallet_transaction_ref", length = 100)
    private String walletTransactionRef;

    @Column(name = "is_surcharge_pending", nullable = false)
    @Builder.Default
    private Boolean isSurchargePending = false;

    @Column(name = "pending_surcharge_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pendingSurchargeAmount = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "weighed_at", nullable = false, updatable = false)
    private Instant weighedAt;
}
