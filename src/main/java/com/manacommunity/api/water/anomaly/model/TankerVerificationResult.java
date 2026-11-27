package com.manacommunity.api.water.anomaly.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TankerVerificationResult {

    public enum Status {
        VERIFIED,
        SHORT_DELIVERY_DETECTED,
        IMPOSSIBLE_DISCHARGE_DURATION,
        GHOST_TANKER_SUSPECTED,
        PHANTOM_DELIVERY_NO_WATER
    }

    public enum PaymentAction {
        APPROVE_FULL_PAYMENT,
        PAY_PRO_RATA,
        WITHHOLD_PAYMENT_AUDIT
    }

    private Long deliveryId;
    private Status status;
    private PaymentAction paymentRecommendation;
    private double invoicedLiters;
    private double calculatedInflowLiters;
    private double discrepancyLiters;
    private double discrepancyPercentage;
    private double decantingDurationMinutes;
    private BigDecimal invoicedCost;
    private BigDecimal approvedPayableAmount;
    private String anomalyReason;
}
