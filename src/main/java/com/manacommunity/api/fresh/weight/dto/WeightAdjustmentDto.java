package com.manacommunity.api.fresh.weight.dto;

import com.manacommunity.api.fresh.weight.model.WeightAdjustmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeightAdjustmentDto {
    private Long id;
    private String adjustmentReference;
    private Long fulfilmentItemId;
    private Long orderId;
    private String orderNumber;
    private Long customerId;
    private String customerName;
    private BigDecimal requestedWeightG;
    private BigDecimal actualWeightG;
    private BigDecimal weightDifferenceG;
    private BigDecimal weightDifferencePct;
    private BigDecimal maxTolerancePct;
    private BigDecimal originalAmount;
    private BigDecimal adjustedAmount;
    private BigDecimal refundOrChargeAmount;
    private WeightAdjustmentStatus adjustmentStatus;
    private String reason;
    private String walletTransactionRef;
    private Boolean isSurchargePending;
    private BigDecimal pendingSurchargeAmount;
    private Instant weighedAt;
}
