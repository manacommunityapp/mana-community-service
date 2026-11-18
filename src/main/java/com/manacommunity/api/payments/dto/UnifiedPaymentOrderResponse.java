package com.manacommunity.api.payments.dto;

import com.manacommunity.api.payments.entity.PaymentOrder;
import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentOrderStatus;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for unified payment order endpoints.
 * Maps from {@link PaymentOrder} — never expose the entity directly.
 */
@Data
@Builder
public class UnifiedPaymentOrderResponse {

    private Long              id;
    private String            idempotencyKey;
    private PaymentGateway    gateway;
    private String            gatewayOrderId;
    private PaymentReferenceType referenceType;
    private Long              referenceId;
    private BigDecimal        amount;
    private String            currency;
    private PaymentOrderStatus status;
    private String            receiptNumber;
    private String            failureReason;
    private BigDecimal        refundedAmount;
    private BigDecimal        convenienceFeeAmount;
    private BigDecimal        grossAmount;
    private OffsetDateTime    createdAt;

    public static UnifiedPaymentOrderResponse from(PaymentOrder o) {
        return UnifiedPaymentOrderResponse.builder()
            .id(o.getId())
            .idempotencyKey(o.getIdempotencyKey())
            .gateway(o.getGateway())
            .gatewayOrderId(o.getGatewayOrderId())
            .referenceType(o.getReferenceType())
            .referenceId(o.getReferenceId())
            .amount(o.getAmount())
            .currency(o.getCurrency())
            .status(o.getStatus())
            .receiptNumber(o.getReceiptNumber())
            .failureReason(o.getFailureReason())
            .refundedAmount(o.getRefundedAmount())
            .convenienceFeeAmount(o.getConvenienceFeeAmount())
            .grossAmount(o.getGrossAmount())
            .createdAt(o.getCreatedAt())
            .build();
    }
}
