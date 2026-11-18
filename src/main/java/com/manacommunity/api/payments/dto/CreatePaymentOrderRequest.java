package com.manacommunity.api.payments.dto;

import com.manacommunity.api.payments.model.PaymentGateway;
import com.manacommunity.api.payments.model.PaymentReferenceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CreatePaymentOrderRequest {
    private Long communityId;
    /** Caller-supplied idempotency key — use UUID or a deterministic hash of business keys. */
    private String idempotencyKey;
    private PaymentGateway gateway;
    private PaymentReferenceType referenceType;
    private Long referenceId;
    private Long payerUserId;
    /** Amount in INR — BigDecimal, never float. */
    private BigDecimal amount;
    @Builder.Default
    private String currency = "INR";
}
